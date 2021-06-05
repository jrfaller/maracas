package org.swat.maracas.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swat.maracas.rest.data.BreakingChangeInstance;
import org.swat.maracas.rest.data.PullRequestResponse;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;

@RestController
@RequestMapping("/github")
public class GithubController {
	private MaracasHelper maracas = MaracasHelper.getInstance();
	private static final String CLONE_PATH = "./clones";
	private static final Logger logger = LogManager.getLogger(GithubController.class);

	@Autowired
    ResourceLoader resourceLoader;

	// Considering the computation time, this should probably be a POST job/GET result duo
	@GetMapping("/pr/{user}/{repository}/{pr}")
	PullRequestResponse analyzePullRequest(@PathVariable String user, @PathVariable String repository, @PathVariable Integer pr) {
		Resource githubRes = resourceLoader.getResource("classpath:.github");
		try (InputStream in = githubRes.getInputStream()) {
			Properties props = new Properties();
			props.load(in);
			// Retrieve PR metadata from GH
			GitHub gh = GitHubBuilder.fromProperties(props).build();
			GHRepository repo = gh.getRepository(user + "/" + repository);
			GHPullRequest pullRequest = repo.getPullRequest(pr);
			GHCommitPointer head = pullRequest.getHead();
			GHCommitPointer base = pullRequest.getBase();
			String headSha = head.getSha();
			String baseSha = base.getSha();
			String headUrl = head.getRepository().getHttpTransportUrl();
			String baseUrl = base.getRepository().getHttpTransportUrl();
			String headRef = head.getRef();
			String baseRef = base.getRef();
			
			// Clone & build the BASE branch
			Path basePath = Paths.get(CLONE_PATH).resolve(baseSha);
			Optional<Path> v1 = cloneAndBuild(baseUrl, baseRef, basePath);

			// Clone & build the HEAD branch
			Path headPath = Paths.get(CLONE_PATH).resolve(headSha);
			Optional<Path> v2 = cloneAndBuild(headUrl, headRef, headPath);
			
			if (v1.isPresent() && v2.isPresent()) {
				Path j1 = v1.get();
				Path j2 = v2.get();
				
				IList delta = maracas.computeDelta(j1, j2, basePath);

				List<BreakingChangeInstance> bcs =
					delta.stream()
						.map(e -> BreakingChangeInstance.fromRascal((IConstructor) e))
						.collect(Collectors.toList());

				return new PullRequestResponse(headRef, baseRef, 0, bcs);
			}
		} catch (IOException | GitAPIException | MavenInvocationException e) {
			logger.error(e);
		}

		return null;
	}

	private Optional<Path> cloneAndBuild(String url, String ref, Path path) throws MavenInvocationException, GitAPIException, IOException {
		// FIXME
		String fullRef = "refs/heads/" + ref;
		if (!path.toFile().exists()) {
			logger.info("Cloning {} [{}]", url, ref);
			Git.cloneRepository()
				.setURI(url)
				.setBranchesToClone(Collections.singletonList(fullRef))
				.setBranch(fullRef)
				.setDirectory(path.toFile())
				.call();
		}
		
		Path target = path.resolve("target");
		if (!target.toFile().exists()) {
			Path pom = path.resolve("pom.xml");

			logger.info("Building {}", pom);
			Properties properties = new Properties();
			properties.setProperty("skipTests", "true");
			
		    InvocationRequest request = new DefaultInvocationRequest();
		    request.setPomFile(pom.toFile());
		    request.setGoals(Collections.singletonList("package"));
		    request.setProperties(properties);
		    request.setBatchMode(true);
		     
		    Invoker invoker = new DefaultInvoker();
		    invoker.setMavenHome(new File("/usr"));
		    invoker.execute(request);
		}
		
	    try (Stream<Path> walk = Files.walk(target, 1)) {
	        return walk.filter(f -> f.toString().endsWith(".jar")).findFirst();
	    }
	}
}
