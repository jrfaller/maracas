package com.github.maracas.rest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.maracas.ClientAnalyzer;
import com.github.maracas.VersionAnalyzer;
import com.github.maracas.delta.Delta;
import com.github.maracas.delta.Detection;
import com.github.maracas.rest.breakbot.BreakbotConfig;
import com.github.maracas.rest.data.ClientDetections;
import com.github.maracas.rest.data.MaracasReport;

@Service
public class MaracasService {
	@Autowired
	private GithubService githubService;

	private static final Logger logger = LogManager.getLogger(MaracasService.class);

	public Delta makeDelta(Path jar1, Path jar2, Path sources) {
		logger.info("Computing delta {} -> {}", jar1, jar2);

		VersionAnalyzer analyzer = new VersionAnalyzer(jar1, jar2);
		analyzer.computeDelta();
		analyzer.populateLocations(sources);
		return analyzer.getDelta();
	}

	public List<Detection> makeDetections(Delta delta, Path clientSources) {
		logger.info("Computing detections on client {}", clientSources);
		ClientAnalyzer clientAnalyzer = new ClientAnalyzer(delta, clientSources, delta.getV1());
		clientAnalyzer.computeDetections();
		return clientAnalyzer.getDetections();
	}

	public MaracasReport makeReport(String repository, String ref, Path basePath, Path jar1, Path jar2, BreakbotConfig config) {
		// Compute delta model
		Path sources = findSourceDirectory(basePath, null);
		Delta maracasDelta = makeDelta(jar1, jar2, sources);

		// Compute detections per client
		List<ClientDetections> detections = new ArrayList<>();
		config.getClients().parallelStream().forEach(c -> {
			try {
				// Clone the client
				String branch = githubService.getBranch(c);
				Path clientPath = githubService.cloneRepository(c);
				Path clientSources = findSourceDirectory(clientPath, c.sources());

				detections.add(
					new ClientDetections(c.repository(),
						makeDetections(maracasDelta, clientSources).stream()
							.map(d -> com.github.maracas.rest.data.Detection.fromMaracasDetection(d, c.repository(), branch, clientPath.toAbsolutePath().toString()))
							.toList())
				);

				logger.info("Done computing detections on {}", c.repository());
			} catch (Exception e) {
				logger.error(e);
				detections.add(new ClientDetections(c.repository(),
					new MaracasException("Couldn't analyze client " + c.repository(), e)));
			}
		});

		return new MaracasReport(
			com.github.maracas.rest.data.Delta.fromMaracasDelta(
				maracasDelta,
				repository,
				ref,
				basePath.toAbsolutePath().toString()
			),
			detections
		);
	}

	protected Path findSourceDirectory(Path basePath, String configSources) {
		Path sources = basePath;
		// User-defined config
		if (configSources != null && !configSources.isEmpty()
			&& basePath.resolve(configSources).toFile().exists())
			sources = basePath.resolve(configSources);
		// Or just try "standard" paths
		else if (basePath.resolve("src/main/java").toFile().exists())
			sources = basePath.resolve("src/main/java");
		else if (basePath.resolve("src/").toFile().exists())
			sources = basePath.resolve("src/");

		return sources;
	}
}
