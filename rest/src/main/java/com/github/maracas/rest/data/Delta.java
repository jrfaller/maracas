package com.github.maracas.rest.data;

import java.nio.file.Path;
import java.util.List;

public record Delta(
	Path jarV1,
	Path jarV2,
	List<BrokenDeclaration> brokenDeclarations
) {
	public static Delta fromMaracasDelta(com.github.maracas.delta.Delta d, String repository, String ref, String clonePath) {
		return new Delta(
			d.getOldJar(),
			d.getNewJar(),
			d.getBrokenDeclarations()
				.stream()
				.map(decl -> BrokenDeclaration.fromMaracasDeclaration(decl, repository, ref, clonePath))
				.toList()
		);
	}
}
