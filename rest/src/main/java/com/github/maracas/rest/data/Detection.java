package com.github.maracas.rest.data;

import java.nio.file.Paths;

import com.github.maracas.SpoonHelper;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtNamedElement;

public record Detection(
	String elem,
	String used,
	String src,
	String apiUse,
	String path,
	int startLine,
	int endLine,
	String url
) {
	public static Detection fromMaracasDetection(com.github.maracas.delta.Detection d, String repository, String ref, String clonePath) {
		SourcePosition pos = d.element().getPosition();

		if (pos instanceof NoSourcePosition)
			return new Detection(
				d.element() instanceof CtNamedElement e ? e.getSimpleName() : d.element().toString(),
				d.usedApiElement() instanceof CtNamedElement e ? e.getSimpleName() : d.usedApiElement().toString(),
				SpoonHelper.fullyQualifiedName(d.source()),
				d.use().name(),
				"",
				-1,
				-1,
				null
			);

		String relativeFile = Paths.get(clonePath).relativize(pos.getFile().toPath()).toString();
		return new Detection(
			d.element() instanceof CtNamedElement e ? e.getSimpleName() : d.element().toString(),
			d.usedApiElement() instanceof CtNamedElement e ? e.getSimpleName() : d.usedApiElement().toString(),
			SpoonHelper.fullyQualifiedName(d.source()),
			d.use().name(),
			relativeFile,
			pos.getLine(),
			pos.getEndLine(),
			repository != null ? GitHubUtils.buildGitHubUrl(repository, ref, relativeFile, pos.getLine(), pos.getEndLine()) : null
		);
	}
}
