package com.github.maracas.visitors;

import com.github.maracas.brokenuse.APIUse;
import com.github.maracas.util.SpoonHelpers;
import japicmp.model.AccessModifier;
import japicmp.model.JApiCompatibilityChange;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

public class ClassLessAccessibleVisitor extends BreakingChangeVisitor {
	private final CtTypeReference<?> clsRef;
	private final AccessModifier newAccessModifier;

	public ClassLessAccessibleVisitor(CtTypeReference<?> clsRef, AccessModifier newAccessModifier) {
		super(JApiCompatibilityChange.CLASS_LESS_ACCESSIBLE);
		this.clsRef = clsRef;
		this.newAccessModifier = newAccessModifier;
	}

	@Override
	public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
		if (clsRef.equals(reference)) {
			APIUse use = getAPIUseByRole(reference);

			String enclosingPkg = SpoonHelpers.getEnclosingPkgName(reference);
			String expectedPkg = SpoonHelpers.getEnclosingPkgName(clsRef.getTypeDeclaration());

			switch (newAccessModifier) {
				// Private always breaks
				case PRIVATE:
					brokenUse(reference.getParent(), reference, clsRef, use);
					break;
				// Package-private breaks if packages do not match
				case PACKAGE_PROTECTED:
					if (!enclosingPkg.equals(expectedPkg))
						brokenUse(reference.getParent(), reference, clsRef, use);
					break;
				// Protected fails if not a subtype and packages do not match
				case PROTECTED:
					if (!reference.getParent(CtType.class).isSubtypeOf(clsRef) &&
						!enclosingPkg.equals(expectedPkg))
						brokenUse(reference.getParent(), reference, clsRef, use);
					break;
				default:
					// Can't happen
			}
		}
	}

	/**
	 * Uncomment if we want to include broken uses for every access to a field
	 * or invocation of a method that is declared by the no-more-visible class.
	 *
	 @Override public <T> void visitCtFieldReference(CtFieldReference<T> reference) {
	 if (clsRef.equals(reference.getDeclaringType()))
	 brokenUse(reference.getParent(), reference.getFieldDeclaration(), clsRef, APIUse.FIELD_ACCESS);
	 }

	 @Override public <T> void visitCtExecutableReference(CtExecutableReference<T> reference) {
	 if (clsRef.equals(reference.getDeclaringType()))
	 brokenUse(reference.getParent(), reference.getExecutableDeclaration(), clsRef, APIUse.METHOD_INVOCATION);
	 }
	 */
}
