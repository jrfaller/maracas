package com.github.maracas.rest.services;

import com.github.maracas.AnalysisResult;
import com.github.maracas.MaracasOptions;
import com.github.maracas.forges.Commit;
import com.github.maracas.forges.CommitBuilder;
import com.github.maracas.forges.Forge;
import com.github.maracas.forges.ForgeAnalyzer;
import com.github.maracas.forges.PullRequest;
import com.github.maracas.forges.Repository;
import com.github.maracas.forges.build.Builder;
import com.github.maracas.forges.build.maven.MavenBuilder;
import com.github.maracas.forges.clone.Cloner;
import com.github.maracas.forges.clone.git.GitCloner;
import com.github.maracas.forges.github.GitHubForge;
import com.github.maracas.rest.breakbot.BreakbotConfig;
import com.github.maracas.rest.data.BrokenUse;
import com.github.maracas.rest.data.ClientReport;
import com.github.maracas.rest.data.MaracasReport;
import com.github.maracas.rest.data.PullRequestResponse;
import japicmp.config.Options;
import japicmp.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Service
public class PullRequestService {
	@Autowired
	private BreakbotService breakbotService;
	@Autowired
	private GitHub github;
	@Value("${maracas.clone-path:./clones}")
	private String clonePath;
	@Value("${maracas.report-path:./reports}")
	private String reportPath;

	private Forge forge;

	private final Map<String, CompletableFuture<Void>> jobs = new ConcurrentHashMap<>();
	private static final Logger logger = LogManager.getLogger(PullRequestService.class);

	@PostConstruct
	public void initialize() {
		Paths.get(clonePath).toFile().mkdirs();
		Paths.get(reportPath).toFile().mkdirs();

		forge = new GitHubForge(github);
	}

	public PullRequest fetchPullRequest(String owner, String repository, int number) {
		return forge.fetchPullRequest(owner, repository, number);
	}

	public String analyzePR(PullRequest pr, String callback, String installationId, String breakbotYaml) {
		BreakbotConfig config =
			StringUtils.isEmpty(breakbotYaml) ?
				breakbotService.readBreakbotConfig(pr.repository().owner(), pr.repository().name()) :
				BreakbotConfig.fromYaml(breakbotYaml);
		String uid = prUid(pr);
		File reportFile = reportFile(pr);
		String reportLocation = "/github/pr/%s/%s/%s".formatted(pr.repository().owner(), pr.repository().name(), pr.number());

		// If we're already on it, no need to compute it twice
		if (isProcessing(pr))
			logger.info("{} is already being analyzed", uid);
		else {
			logger.info("Starting the analysis of {}", uid);

			CompletableFuture<Void> future =
				CompletableFuture
					.supplyAsync(() -> buildMaracasReport(pr, config))
					.handle((report, ex) -> {
						jobs.remove(uid);

						if (ex != null) {
							logger.error("Error analyzing " + uid, ex);
							return new PullRequestResponse(ex.getCause().getMessage());
						}

						logger.info("Done analyzing {}", uid);
						serializeReport(report, reportFile);
						return new PullRequestResponse("ok", report);
					})
					.thenAccept(response -> {
						if (callback != null)
							breakbotService.sendPullRequestResponse(response, callback, installationId);
					});

			jobs.put(uid, future);
		}

		return reportLocation;
	}

	public MaracasReport analyzePRSync(PullRequest pr, String breakbotYaml) {
		BreakbotConfig config =
			StringUtils.isEmpty(breakbotYaml) ?
				breakbotService.readBreakbotConfig(pr.repository().owner(), pr.repository().name()) :
				BreakbotConfig.fromYaml(breakbotYaml);
		return buildMaracasReport(pr, config);
	}

	private MaracasReport buildMaracasReport(PullRequest pr, BreakbotConfig config) {
		try {
			Properties buildProperties = new Properties();
			config.build().properties().forEach(p -> buildProperties.put(p, "true"));

			Path baseClone = clonePath(pr.base());
			CommitBuilder baseBuilder = new CommitBuilder(pr.base(), baseClone);
			baseBuilder.setBuildGoals(config.build().goals());
			baseBuilder.setBuildProperties(buildProperties);
			if (!StringUtils.isEmpty(config.build().sources())) {
				Path sources = baseClone.resolve(config.build().sources());
				if (sources.toFile().exists())
					baseBuilder.setSources(sources);
			}

			Path headClone = clonePath(pr.head());
			CommitBuilder headBuilder = new CommitBuilder(pr.head(), headClone);
			headBuilder.setBuildGoals(config.build().goals());
			headBuilder.setBuildProperties(buildProperties);

			Map<Path, CommitBuilder> clientBuilders = new HashMap<>();
			for (BreakbotConfig.GitHubRepository c : config.clients()) {
				Repository clientRepo =
					StringUtils.isEmpty(c.branch()) ?
						forge.fetchRepository(c.owner(), c.name()) :
						forge.fetchRepository(c.owner(), c.name(), c.branch());

				// Argh
				String clientSha = "";
				try {
					clientSha = github.getRepository(c.owner() + "/" + c.name()).getBranch(clientRepo.branch()).getSHA1();
				} catch (IOException e) {}

				Commit clientCommit =
					StringUtils.isEmpty(c.sha()) ?
						new Commit(clientRepo, clientSha) :
						new Commit(clientRepo, c.sha());
				Path clientClone = clonePath(clientCommit);
				CommitBuilder clientBuilder = new CommitBuilder(clientCommit, clientClone);
				if (!StringUtils.isEmpty(c.sources())) {
					Path sources = baseClone.resolve(config.build().sources());
					if (sources.toFile().exists())
						clientBuilder.setSources(sources);
				}

				clientBuilders.put(clientClone, clientBuilder);
			}

			MaracasOptions options = MaracasOptions.newDefault();
			Options jApiOptions = options.getJApiOptions();
			config.excludes().forEach(excl -> jApiOptions.addExcludeFromArgument(Optional.of(excl), false));
			ForgeAnalyzer analyzer = new ForgeAnalyzer();
			AnalysisResult result = analyzer.analyzeCommits(baseBuilder, headBuilder, clientBuilders.values().stream().toList(), options);

			return new MaracasReport(
				com.github.maracas.rest.data.Delta.fromMaracasDelta(
					result.delta(),
					pr,
					baseClone
				),
				result.deltaImpacts().values().stream()
					.map(impact -> {
						CommitBuilder builder = clientBuilders.get(impact.getClient());
						Repository clientRepo = builder.getCommit().repository();
						String clientName = clientRepo.owner() + "/" + clientRepo.name();
						Throwable t = impact.getError();

						if (t != null)
							return ClientReport.error(clientName, t);
						else
							return ClientReport.success(clientName,
								impact.getBrokenUses().stream()
									.map(bu -> BrokenUse.fromMaracasBrokenUse(bu, clientRepo, clientRepo.branch(), impact.getClient()))
									.toList());
					})
					.toList()
			);
		} catch (ExecutionException | InterruptedException e) {
			logger.error(e);
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public Builder cloneAndBuild(Commit c, BreakbotConfig.Build config) {
		MavenBuilder builder = new MavenBuilder(clone(c).resolve(config.pom()));
		Properties properties = new Properties();
		config.properties().forEach(p -> properties.put(p, "true"));
		builder.build(config.goals(), properties);
		return builder;
	}

	public Path clone(Commit c) {
		Cloner cloner = new GitCloner();
		return cloner.clone(c, clonePath(c));
	}

	public Path locateSources(Path base, String sources) {
		if (!StringUtils.isEmpty(sources) && base.resolve(sources).toFile().exists())
			return base.resolve(sources);
		else if (base.resolve("src/main/java").toFile().exists())
			return base.resolve("src/main/java");
		else if (base.resolve("src/").toFile().exists())
			return base.resolve("src");
		else
			return base;
	}

	public boolean isProcessing(PullRequest pr) {
		return jobs.containsKey(prUid(pr));
	}

	private void serializeReport(MaracasReport report, File reportFile) {
		try {
			logger.info("Serializing {}", reportFile);
			reportFile.getParentFile().mkdirs();
			report.writeJson(reportFile);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public MaracasReport getReport(PullRequest pr) {
		try {
			File reportFile = reportFile(pr);
			if (reportFile.exists() && reportFile.length() > 0) {
				return MaracasReport.fromJson(reportFile);
			}
		} catch (IOException e) {
			logger.error(e);
		}

		return null;
	}

	private String prUid(PullRequest pr) {
		return "%s-%s-%s-%s".formatted(
			pr.repository().owner(),
			pr.repository().name(),
			pr.number(),
			pr.head().sha()
		);
	}

	private File reportFile(PullRequest pr) {
		return Paths.get(reportPath)
			.resolve(pr.repository().owner())
			.resolve(pr.repository().name())
			.resolve("%d-%s.json".formatted(pr.number(), pr.head().sha()))
			.toFile();
	}

	private Path clonePath(Commit c) {
		return Paths.get(clonePath)
			.resolve(c.repository().owner())
			.resolve(c.repository().name())
			.resolve(c.sha())
			.toAbsolutePath();
	}
}
