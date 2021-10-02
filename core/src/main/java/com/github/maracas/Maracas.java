package com.github.maracas;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.maracas.delta.BrokenDeclaration;
import com.github.maracas.delta.Delta;
import com.github.maracas.detection.Detection;
import com.github.maracas.util.PathHelpers;
import com.github.maracas.visitors.BreakingChangeVisitor;
import com.github.maracas.visitors.CombinedVisitor;

import japicmp.cli.JApiCli.ClassPathMode;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.AccessModifier;
import japicmp.model.JApiClass;
import japicmp.output.OutputFilter;
import japicmp.util.Optional;
import spoon.Launcher;
import spoon.reflect.CtModel;

public class Maracas {
	// Just use the static methods
	private Maracas() {

	}

	/**
	 * Analyzes the given {@code query}
	 *
	 * @param query The query to analyze
	 * @return the resulting {@link AnalysisResult} with delta and detections
	 * @throws NullPointerException if query is null
	 */
	public static AnalysisResult analyze(AnalysisQuery query) {
		Objects.requireNonNull(query);

		// Compute the delta model between old and new JARs
		Delta delta = computeDelta(query.getOldJar(), query.getNewJar());

		// If we get the library's sources, populate the delta's source code locations
		if (query.getSources() != null)
			delta.populateLocations(query.getSources());

		// For every client, compute the set of detections
		Map<Path, Collection<Detection>> clientsDetections = new HashMap<>();
		query.getClients()
			.forEach(c ->
				clientsDetections.put(c, computeDetections(c, delta))
			);

		return new AnalysisResult(delta, clientsDetections);
	}

	/**
	 * Compares the library's old and new JARs and returns a delta model
	 * containing all {@link BrokenDeclaration} between them, based on JApiCmp.
	 *
	 * @param oldJar The library's old JAR
	 * @param newJar The library's new JAR
	 * @return a new delta model based on JapiCmp's results
	 * @see JarArchiveComparator#compare(JApiCmpArchive, JApiCmpArchive)
	 * @throws IllegalArgumentException if oldJar or newJar aren't valid
	 */
	public static Delta computeDelta(Path oldJar, Path newJar) {
		if (!PathHelpers.isValidJar(oldJar))
			throw new IllegalArgumentException("oldJar isn't a valid JAR: " + oldJar);
		if (!PathHelpers.isValidJar(newJar))
			throw new IllegalArgumentException("newJar isn't a valid JAR: " + newJar);

		Options defaultOptions = getJApiOptions();
		JarArchiveComparatorOptions options = JarArchiveComparatorOptions.of(defaultOptions);
		JarArchiveComparator comparator = new JarArchiveComparator(options);

		JApiCmpArchive oldAPI = new JApiCmpArchive(oldJar.toFile(), "v1");
		JApiCmpArchive newAPI = new JApiCmpArchive(newJar.toFile(), "v2");

		List<JApiClass> classes = comparator.compare(oldAPI, newAPI);

		OutputFilter filter = new OutputFilter(defaultOptions);
		filter.filter(classes);

		return Delta.fromJApiCmpDelta(
			oldJar.toAbsolutePath(), newJar.toAbsolutePath(), classes);
	}

	/**
	 * Computes the impact the {@code delta} model has on {@code client} and
	 * returns the corresponding list of {@link Detection}
	 *
	 * @param client Valid path to the client's source code to analyze
	 * @param delta The delta model
	 * @return the corresponding list of {@link Detection}
	 * @throws NullPointerException if delta is null
	 * @throws IllegalArgumentException if client isn't a valid directory
	 */
	public static Collection<Detection> computeDetections(Path client, Delta delta) {
		Objects.requireNonNull(delta);
		if (!PathHelpers.isValidDirectory(client))
			throw new IllegalArgumentException("client isn't a valid directory: " + client);

		Launcher launcher = new Launcher();
		launcher.addInputResource(client.toAbsolutePath().toString());
		String[] javaCP = { delta.getOldJar().toAbsolutePath().toString() };
		launcher.getEnvironment().setSourceClasspath(javaCP);
		CtModel model = launcher.buildModel();

		List<BreakingChangeVisitor> visitors = delta.getVisitors();
		CombinedVisitor visitor = new CombinedVisitor(visitors);

		visitor.scan(model.getRootPackage());

		return visitor.getDetections();
	}

	private static Options getJApiOptions() {
		Options defaultOptions = Options.newDefault();
		defaultOptions.setAccessModifier(AccessModifier.PROTECTED);
		defaultOptions.setOutputOnlyModifications(true);
		defaultOptions.setClassPathMode(ClassPathMode.TWO_SEPARATE_CLASSPATHS);
		defaultOptions.setIgnoreMissingClasses(false);

		// FIXME: inherited from maracas-rascal
		String[] excl = { "(*.)?tests(.*)?", "(*.)?test(.*)?",
				"@org.junit.After",
				"@org.junit.AfterClass",
				"@org.junit.Before",
				"@org.junit.BeforeClass",
				"@org.junit.Ignore",
				"@org.junit.Test",
				"@org.junit.runner.RunWith" };

		for (String e : excl) {
			defaultOptions.addExcludeFromArgument(Optional.of(e), false);
		}

		return defaultOptions;
	}
}
