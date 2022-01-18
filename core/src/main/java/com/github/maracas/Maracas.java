package com.github.maracas;

import com.github.maracas.brokenUse.BrokenUse;
import com.github.maracas.delta.BreakingChange;
import com.github.maracas.delta.Delta;
import com.github.maracas.util.PathHelpers;
import com.github.maracas.util.SpoonHelpers;
import com.github.maracas.visitors.BreakingChangeVisitor;
import com.github.maracas.visitors.CombinedVisitor;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibilityChange;
import japicmp.output.OutputFilter;
import spoon.reflect.CtModel;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Maracas {
	// Just use the static methods
	private Maracas() {

	}

	/**
	 * Analyzes the given {@code query}
	 *
	 * @param query The query to analyze
	 * @return the resulting {@link AnalysisResult} with delta and broken uses
	 * @throws NullPointerException if query is null
	 */
	public static AnalysisResult analyze(AnalysisQuery query) {
		Objects.requireNonNull(query);

		// Compute the delta model between old and new JARs
		Delta delta = computeDelta(query.getOldJar(), query.getNewJar(), query.getMaracasOptions());

		// If we get the library's sources, populate the delta's source code locations
		if (query.getSources() != null)
			delta.populateLocations(query.getSources());

		// For every client, compute the set of broken uses
		Map<Path, Collection<BrokenUse>> clientsBrokenUses = new HashMap<>();
		query.getClients()
			.forEach(c ->
				clientsBrokenUses.put(c, computeBrokenUses(c, delta))
			);

		return new AnalysisResult(delta, clientsBrokenUses);
	}

	/**
	 * Compares the library's old and new JARs and returns a delta model
	 * containing all {@link BreakingChange} between them, based on JApiCmp.
	 *
	 * @param oldJar The library's old JAR
	 * @param newJar The library's new JAR
	 * @param options Maracas and JApiCmp options
	 * @return a new delta model based on JapiCmp's results
	 * @see JarArchiveComparator#compare(JApiCmpArchive, JApiCmpArchive)
	 * @see #computeDelta(Path, Path, MaracasOptions)
	 * @throws IllegalArgumentException if oldJar or newJar aren't valid
	 */
	public static Delta computeDelta(Path oldJar, Path newJar, MaracasOptions options) {
		if (!PathHelpers.isValidJar(oldJar))
			throw new IllegalArgumentException("oldJar isn't a valid JAR: " + oldJar);
		if (!PathHelpers.isValidJar(newJar))
			throw new IllegalArgumentException("newJar isn't a valid JAR: " + newJar);

		MaracasOptions opts = options != null ? options : defaultMaracasOptions();
		JarArchiveComparatorOptions jApiOptions = JarArchiveComparatorOptions.of(opts.getJApiOptions());
		JarArchiveComparator comparator = new JarArchiveComparator(jApiOptions);

		JApiCmpArchive oldAPI = new JApiCmpArchive(oldJar.toFile(), "v1");
		JApiCmpArchive newAPI = new JApiCmpArchive(newJar.toFile(), "v2");

		List<JApiClass> classes = comparator.compare(oldAPI, newAPI);

		OutputFilter filter = new OutputFilter(opts.getJApiOptions());
		filter.filter(classes);

		return Delta.fromJApiCmpDelta(
			oldJar.toAbsolutePath(), newJar.toAbsolutePath(), classes, options);
	}

	/**
	 * @see #computeDelta(Path, Path, MaracasOptions)
	 */
	public static Delta computeDelta(Path oldJar, Path newJar) {
		return computeDelta(oldJar, newJar, defaultMaracasOptions());
	}

	/**
	 * Computes the impact the {@code delta} model has on {@code client} and
	 * returns the corresponding list of {@link BrokenUse}
	 *
	 * @param client Valid path to the client's source code to analyze
	 * @param delta The delta model
	 * @return the corresponding list of {@link BrokenUse}
	 * @throws NullPointerException if delta is null
	 * @throws IllegalArgumentException if client isn't a valid directory
	 */
	public static Collection<BrokenUse> computeBrokenUses(Path client, Delta delta) {
		Objects.requireNonNull(delta);
		if (!PathHelpers.isValidDirectory(client))
			throw new IllegalArgumentException("client isn't a valid directory: " + client);

		CtModel model = SpoonHelpers.buildSpoonModel(client, delta.getOldJar());

		Collection<BreakingChangeVisitor> visitors = delta.getVisitors();
		CombinedVisitor visitor = new CombinedVisitor(visitors);

		// FIXME: Only way I found to visit CompilationUnits and Imports in the model
		// This is probably not the right way.
		// We still need to visit the root package afterwards.
		visitor.scan(model.getRootPackage().getFactory().CompilationUnit().getMap());
		visitor.scan(model.getRootPackage());

		return visitor.getBrokenUses();
	}

	public static MaracasOptions defaultMaracasOptions() {
		MaracasOptions maracasOptions = MaracasOptions.newDefault();

		// We don't care about source- and binary-compatible changes (except ADA...)
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.SUPERCLASS_ADDED);
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.INTERFACE_ADDED);
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.METHOD_ADDED_TO_PUBLIC_CLASS);
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.METHOD_ABSTRACT_ADDED_IN_IMPLEMENTED_INTERFACE);

		// We don't care about _IN_SUPERCLASS changes as they're just redundant
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.METHOD_REMOVED_IN_SUPERCLASS);
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.FIELD_LESS_ACCESSIBLE_THAN_IN_SUPERCLASS);
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.FIELD_REMOVED_IN_SUPERCLASS);
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.METHOD_ABSTRACT_ADDED_IN_SUPERCLASS);
		maracasOptions.excludeBreakingChange(JApiCompatibilityChange.METHOD_DEFAULT_ADDED_IN_IMPLEMENTED_INTERFACE);

		return maracasOptions;
	}
}
