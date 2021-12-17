package com.github.maracas.visitors;

import java.util.HashSet;
import java.util.Set;

import com.github.maracas.brokenUse.APIUse;
import com.github.maracas.brokenUse.BrokenUse;
import com.github.maracas.util.SpoonHelpers;

import japicmp.model.JApiCompatibilityChange;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtReference;
import spoon.reflect.visitor.CtAbstractVisitor;

/**
 * Abstract visitor in charge of gathering broken uses in client code.
 */
public abstract class BreakingChangeVisitor extends CtAbstractVisitor {
    /**
     * Kind of breaking change as defined by JApiCmp.
     */
	protected final JApiCompatibilityChange change;

	/**
	 * Set of detected broken uses.
	 */
	protected final Set<BrokenUse> brokenUses = new HashSet<>();

	/**
	 * Creates a BreakingChangeVisitor instance.
	 *
	 * The constructor first invokes the constructor of the superclass.
	 * @see   <a href="https://javadoc.io/static/fr.inria.gforge.spoon/spoon-core/7.3.0/spoon/reflect/visitor/CtAbstractVisitor.html">
	 *        spoon.reflect.visitor.CtAbstractVisitor</a>
	 * @param change  kind of breaking change as defined by JApiCmp
	 */
	protected BreakingChangeVisitor(JApiCompatibilityChange change) {
		super();
		this.change = change;
	}

	/**
     * Returns the set of detected broken uses.
     * @return set of broken uses
     */
    public Set<BrokenUse> getBrokenUses() {
        return brokenUses;
    }

	/**
	 * Add a new broken use to the set of detected broken uses.
	 *
	 * @param element        client code impacted by the breaking change
	 * @param usedApiElement API declaration used by the impacted client code
	 * @param source         API declaration that introduced the breaking change
	 * @param use            type of use of the API declaration
	 */
	protected void brokenUse(CtElement element, CtElement usedApiElement, CtReference source, APIUse use) {
		// We don't want to create broken uses for implicit elements: they do not
		// exist in the source code of the client anyway
		if (element.isImplicit())
			return;

		// In case we don't get a source code position for the element, we default
		// to the first parent that can be located
		CtElement locatableElement =
			element.getPosition() instanceof NoSourcePosition ?
				SpoonHelpers.firstLocatableParent(element) :
				element;

		BrokenUse d = new BrokenUse(
			locatableElement,
			usedApiElement,
			source,
			use,
			change
		);

		brokenUses.add(d);
	}

	/**
	 * Identifies the type of use of the API declaration given a Spoon element.
	 * @see <a href="https://spoon.gforge.inria.fr/mvnsites/spoon-core/apidocs/spoon/reflect/declaration/CtElement.html">
	 *      CtElement</a>
	 * @param element Spoon element whose use type needs to be identified
	 * @return        type of use of the API element
	 */
	public APIUse getAPIUseByRole(CtElement element) {
		CtRole role = element.getRoleInParent();
		return switch (role) {
			// FIXME: try to distinguish between regular access to a type,
			// and access to a type by instantiation (new)
			case CAST, DECLARING_TYPE, TYPE, ARGUMENT_TYPE, ACCESSED_TYPE, TYPE_ARGUMENT, THROWN, MULTI_TYPE ->
				APIUse.TYPE_DEPENDENCY;
			case SUPER_TYPE ->
				APIUse.EXTENDS;
			case INTERFACE ->
				APIUse.IMPLEMENTS;
			case ANNOTATION_TYPE ->
				APIUse.ANNOTATION;
			case IMPORT_REFERENCE ->
				APIUse.IMPORT;
			case DECLARED_TYPE_REF ->
				APIUse.TYPE_DEPENDENCY; // FIXME: This one is weird
			case BOUNDING_TYPE ->
			    APIUse.TYPE_DEPENDENCY;
			default ->
				throw new RuntimeException("Unmanaged role " + role + " for " + element + " in " + element.getParent());
		};
	}
}
