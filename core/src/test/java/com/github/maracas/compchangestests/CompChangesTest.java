package com.github.maracas.compchangestests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;

import com.github.maracas.AnalysisQuery;
import com.github.maracas.AnalysisResult;
import com.github.maracas.Maracas;
import com.github.maracas.detection.APIUse;
import com.github.maracas.detection.Detection;
import com.github.maracas.util.SpoonHelpers;

import japicmp.model.JApiCompatibilityChange;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;

public class CompChangesTest {
	static Collection<Detection> detections;

	@BeforeAll
	static void setUp() {
		Path v1 = Paths.get("../test-data/comp-changes/old/target/comp-changes-old-0.0.1.jar");
		Path v2 = Paths.get("../test-data/comp-changes/new/target/comp-changes-new-0.0.1.jar");
		Path client = Paths.get("../test-data/comp-changes/client/src/");

		AnalysisQuery query = AnalysisQuery.builder()
			.oldJar(v1)
			.newJar(v2)
			.client(client)
			.build();

		AnalysisResult result = Maracas.analyze(query);
		detections = result.allDetections();
	}

	public static void assertDetection(String file, int line, JApiCompatibilityChange change, APIUse use) {
		Optional<Detection> find =
			detections.stream().filter(d -> {
				if (change != d.change())
					return false;
				if (use != d.use())
					return false;

				SourcePosition pos = SpoonHelpers.firstLocatableParent(d.element()).getPosition();
				if (pos instanceof NoSourcePosition)
					return false;
				if (!file.equals(pos.getFile().getName().toString()))
					return false;
				if (line != pos.getLine())
					return false;

				return true;
			}).findAny();

		assertTrue(
			find.isPresent(),
			String.format("No detection found in %s:%d [%s] [%s]",
				file, line, change, use)
		);
	}

	public static void assertNumberDetections(JApiCompatibilityChange change, int n) {
		List<Detection> ds = detections.stream().filter(d -> d.change() == change).collect(Collectors.toList());

		assertEquals(n, ds.size(), ds.toString());
	}
}
