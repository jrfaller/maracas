package com.github.maracas.util;

import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A BinaryToSourceMapper attempts to map Spoon's {@link CtReference}
 * extracted from binary code (typically the old JAR) to the corresponding
 * {@link CtElement} in the source code version of this JAR.
 */
public final class BinaryToSourceMapper {
	private final CtPackage root;
	private final Map<CtReference, CtElement> mapped = new HashMap<>();

	public BinaryToSourceMapper(CtPackage root) {
		this.root = root;
	}

	public CtElement resolve(CtReference binaryRef) {
		if (mapped.containsKey(binaryRef))
			return mapped.get(binaryRef);

		if (binaryRef instanceof CtTypeReference<?> typeRef) {
			return cache(typeRef, resolve(typeRef));
		} else if (binaryRef instanceof CtExecutableReference<?> execRef) {
			return cache(execRef, resolve(execRef));
		} else if (binaryRef instanceof CtFieldReference<?> fieldRef) {
			return cache(fieldRef, resolve(fieldRef));
		}

		return null;
	}

	private CtElement cache(CtReference binaryRef, CtElement resolved) {
		mapped.put(binaryRef, resolved);
		return resolved;
	}

	public CtType<?> resolve(CtTypeReference<?> binaryRef) {
		CtType<?> binaryDecl = binaryRef.getTypeDeclaration();

		if (binaryDecl != null) {
			CtTypeReference<?> sourceRef = root.getFactory().Type().createReference(binaryDecl);
			CtType<?> typeDecl = sourceRef.getTypeDeclaration();

			if (typeDecl != null && typeDecl.getPosition().isValidPosition())
				return typeDecl;
		}

		return null;
	}

	public CtExecutable<?> resolve(CtExecutableReference<?> binaryRef) {
		CtExecutable<?> binaryExec = binaryRef.getExecutableDeclaration();

		if (binaryExec != null) {
			CtType<?> declaringType = resolve(binaryRef.getDeclaringType());

			if (declaringType != null) {
				Optional<CtExecutableReference<?>> sourceRefOpt =
					declaringType.getDeclaredExecutables().stream()
						.filter(e -> Objects.equals(e.getSignature(), binaryRef.getSignature()))
						.findFirst();

				if (sourceRefOpt.isPresent()) {
					CtExecutableReference<?> sourceRef = sourceRefOpt.get();
					CtExecutable<?> execDecl = sourceRef.getExecutableDeclaration();

					if (execDecl != null && execDecl.getPosition().isValidPosition())
						return execDecl;
				}
			}
		}

		return null;
	}

	public CtField<?> resolve(CtFieldReference<?> binaryRef) {
		CtField<?> binaryField = binaryRef.getFieldDeclaration();

		if (binaryField != null) {
			CtType<?> declaringType = resolve(binaryRef.getDeclaringType());

			if (declaringType != null) {
				CtFieldReference<?> sourceRef = declaringType.getDeclaredField(binaryRef.getSimpleName());
				CtField<?> fieldDecl = sourceRef.getFieldDeclaration();

				if (fieldDecl != null && fieldDecl.getPosition().isValidPosition())
					return fieldDecl;
			}
		}

		return null;
	}
}
