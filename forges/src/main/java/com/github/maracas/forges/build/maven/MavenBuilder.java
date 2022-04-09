package com.github.maracas.forges.build.maven;

import com.github.maracas.forges.build.BuildException;
import com.github.maracas.forges.build.Builder;
import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class MavenBuilder implements Builder {
	private final Path pom;
	private final Path target;
	private final Path base;

	public static final Properties DEFAULT_PROPERTIES = new Properties();
	public static final List<String> DEFAULT_GOALS = new ArrayList<>();
	private static final Logger logger = LogManager.getLogger(MavenBuilder.class);

	static {
		DEFAULT_PROPERTIES.setProperty("maven.test.skip", "true");
		DEFAULT_PROPERTIES.setProperty("assembly.skipAssembly", "true");
		DEFAULT_GOALS.add("package");
	}

	public MavenBuilder(Path pom) {
		Objects.requireNonNull(pom);
		if (!pom.toFile().exists())
			throw new BuildException("The pom file doesn't exist: " + pom);

		this.pom = pom.toAbsolutePath();
		this.base = pom.getParent().toAbsolutePath();
		this.target = base.resolve("target").toAbsolutePath();
	}

	@Override
	public void build() {
		build(DEFAULT_GOALS, DEFAULT_PROPERTIES);
	}

	@Override
	public void build(List<String> goals, Properties properties) {
		Objects.requireNonNull(goals);
		Objects.requireNonNull(properties);

		Optional<Path> jar = locateJar();
		if (jar.isEmpty()) {
			logger.info("Building {} with goals={} properties={}",
				pom, goals, properties);

			Stopwatch sw = Stopwatch.createStarted();
			StringBuilder errors = new StringBuilder();
			InvocationRequest request = new DefaultInvocationRequest();
			request.setPomFile(pom.toFile());
			request.setGoals(goals);
			request.setProperties(properties);
			request.setBatchMode(true);
			request.setQuiet(true);
			// For some reason, every handler but setOutputHandler is ignored
			// Here, invoked only with errors because quiet == true
			request.setOutputHandler(line -> {
				logger.error(line);
				errors.append(line);
			});

			try {
				Invoker invoker = new DefaultInvoker();
				InvocationResult result = invoker.execute(request);

				if (result.getExecutionException() != null)
					throw new BuildException("%s failed: %s".formatted(goals, result.getExecutionException().getMessage()));
				if (result.getExitCode() != 0)
					throw new BuildException("%s failed (%d): %s".formatted(goals, result.getExitCode(), errors.toString()));

				logger.info("Building {} with goals={} properties={} took {}ms",
					pom, goals, properties, sw.elapsed().toMillis());
			} catch (MavenInvocationException e) {
				throw new BuildException("Error invoking Maven", e);
			}
		} else logger.info("{} has already been built. Skipping.", pom);
	}

	@Override
	public Optional<Path> locateJar() {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		try (InputStream in = new FileInputStream(pom.toFile())) {
			Model model = reader.read(in);
			String aid = model.getArtifactId();
			String vid = model.getVersion();
			Path jar = target.resolve("%s-%s.jar".formatted(aid, vid));

			if (jar.toFile().exists())
				return Optional.of(jar);
			else
				return Optional.empty();
		} catch (IOException | XmlPullParserException e) {
			throw new BuildException("Couldn't parse " + pom, e);
		}
	}
}
