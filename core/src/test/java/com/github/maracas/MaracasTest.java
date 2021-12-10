package com.github.maracas;

import com.github.maracas.brokenUse.BrokenUse;
import com.github.maracas.delta.Delta;

import japicmp.config.Options;
import japicmp.model.AccessModifier;
import org.junit.jupiter.api.Test;
import spoon.reflect.cu.position.NoSourcePosition;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaracasTest {
	Path v1 = Paths.get("../test-data/comp-changes/old/target/comp-changes-old-0.0.1.jar");
	Path v2 = Paths.get("../test-data/comp-changes/new/target/comp-changes-new-0.0.1.jar");
	Path client = Paths.get("../test-data/comp-changes/client/src/");
	Path client2 = Paths.get("../test-data/comp-changes/old/src/");
	Path sources = Paths.get("../test-data/comp-changes/old/src/");

	@Test
	void analyze_QueryWithoutClient_hasNoBrokenUse() {
		AnalysisResult res = Maracas.analyze(
			AnalysisQuery.builder()
				.oldJar(v1)
				.newJar(v2)
				.build());

		assertThat(res.delta(), is(notNullValue()));
		assertThat(res.delta().getBreakingChanges(),
			everyItem(allOf(
				hasProperty("reference", is(notNullValue())),
				hasProperty("sourceElement", is(nullValue()))
			)));
		assertThat(res.allBrokenUses(), is(empty()));
	}

	@Test
	void analyze_QueryWithTwoClients_hasTwoClientBrokenUses() {
		AnalysisResult res = Maracas.analyze(
			AnalysisQuery.builder()
				.oldJar(v1)
				.newJar(v2)
				.client(client)
				.client(client2)
				.build());

		assertThat(res.delta(), is(notNullValue()));
		assertThat(res.brokenUses().keySet(), hasSize(2));
		assertThat(res.brokenUses(), hasKey(client.toAbsolutePath()));
		assertThat(res.brokenUses(), hasKey(client2.toAbsolutePath()));
	}

	@Test
	void analyze_QueryWithSources_hasSourceLocations() {
		AnalysisResult res = Maracas.analyze(
			AnalysisQuery.builder()
				.oldJar(v1)
				.newJar(v2)
				.sources(sources)
				.build());

		assertThat(res.delta(), is(notNullValue()));
		assertThat(res.delta().getBreakingChanges(),
			everyItem(hasProperty("sourceElement", is(notNullValue()))));
	}

	@Test
	void analyze_QueryWithOptions_UsesOptions() {
		Options publicOpts = Maracas.defaultJApiOptions();
		publicOpts.setAccessModifier(AccessModifier.PUBLIC);

		Options privateOpts = Maracas.defaultJApiOptions();
		privateOpts.setAccessModifier(AccessModifier.PRIVATE);

		AnalysisResult resPublic = Maracas.analyze(
			AnalysisQuery.builder()
				.oldJar(v1)
				.newJar(v2)
				.jApiOptions(publicOpts)
				.build());

		AnalysisResult resPrivate = Maracas.analyze(
			AnalysisQuery.builder()
				.oldJar(v1)
				.newJar(v2)
				.jApiOptions(privateOpts)
				.build());

		assertThat(resPublic.delta().getBreakingChanges().size(),
			is(not(equalTo(resPrivate.delta().getBreakingChanges().size()))));
	}

	@Test
	void computeDelta_isValid() {
		Delta d = Maracas.computeDelta(v1, v2);

		assertThat(d, is(notNullValue()));
		assertThat(d.getOldJar(), is(equalTo(v1.toAbsolutePath())));
		assertThat(d.getNewJar(), is(equalTo(v2.toAbsolutePath())));
		assertThat(d.getBreakingChanges(), everyItem(allOf(
			hasProperty("reference", is(notNullValue())),
			// TODO: uncomment once all visitors are implemented
			//hasProperty("visitor", is(notNullValue()))
			hasProperty("sourceElement", is(nullValue()))
		)));

		d.populateLocations(sources);
		assertThat(d.getBreakingChanges(), everyItem(
			hasProperty("sourceElement", allOf(
				is(notNullValue()),
				hasProperty("position", is(not(instanceOf(NoSourcePosition.class))))
			)
		)));
	}

	@Test
	void computeBrokenUses_isValid() {
		Delta delta = Maracas.computeDelta(v1, v2);
		Collection<BrokenUse> ds = Maracas.computeBrokenUses(client, delta);

		assertThat(ds, is(not(empty())));
		// No hasProperty() on records :(
		ds.forEach(d -> {
			assertThat(d.element(), allOf(
				is(notNullValue()),
				hasProperty("position", is(not(instanceOf(NoSourcePosition.class))))));
			assertThat(d.usedApiElement(), is(notNullValue()));
			assertThat(d.source(), is(notNullValue()));
		});
	}

	@Test
	void analyze_aNullQuery_throwsException() {
		assertThrows(NullPointerException.class, () ->
			Maracas.analyze(null)
		);
	}

	@Test
	void computeDelta_invalidPaths_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
			Maracas.computeDelta(v1, null)
		);

		assertThrows(IllegalArgumentException.class, () ->
			Maracas.computeDelta(null, v2)
		);

		assertThrows(IllegalArgumentException.class, () ->
			Maracas.computeDelta(v1, TestData.invalidJar)
		);

		assertThrows(IllegalArgumentException.class, () ->
			Maracas.computeDelta(TestData.invalidJar, v2)
		);
	}

	@Test
	void computeBrokenUses_invalidPaths_throwsException() {
		Delta d = Maracas.computeDelta(v1, v2);
		assertThrows(IllegalArgumentException.class, () ->
			Maracas.computeBrokenUses(TestData.invalidDirectory, d)
		);

		assertThrows(IllegalArgumentException.class, () ->
			Maracas.computeBrokenUses(null, d)
		);
	}

	@Test
	void computeBrokenUses_nullDelta_throwsException() {
		assertThrows(NullPointerException.class, () ->
			Maracas.computeBrokenUses(v1, null)
		);
	}

}
