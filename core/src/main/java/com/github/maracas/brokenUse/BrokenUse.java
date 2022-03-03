package com.github.maracas.brokenUse;

import java.util.Objects;

import japicmp.model.JApiCompatibilityChange;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.reference.CtReference;

/**
 * A broken use is a source code location in client code that is impacted
 * by a breaking change.
 */
public record BrokenUse(
	/**
	 * The impacted {@link CtElement} in the client's AST
	 */
	CtElement element,

	/**
	 * The {@link CtElement} in the library's code that is being used by the {@link #element}
	 */
	CtElement usedApiElement,

	/**
	 * The original breaking change in the library's code
	 */
	CtReference source,

	/**
	 * The kind of use relationship between the {@link #element} and {@link #usedApiElement}
	 */
	APIUse use,

	/**
	 * The kind of breaking change {@link #source}
	 */
	JApiCompatibilityChange change
) {
	@Override
	public String toString() {
		return """
		[%s]
			Element: %s (%s:%d)
			Used:    %s
			Source:  %s
			Use:     %s\
		""".formatted(
			change,
			element instanceof CtNamedElement namedElement ? namedElement.getSimpleName() :	element.toString(),
			element.getPosition() instanceof NoSourcePosition ? "unknown" : element.getPosition().getFile().getName(),
			element.getPosition() instanceof NoSourcePosition ? -1 : element.getPosition().getLine(),
			usedApiElement instanceof CtNamedElement namedUsedApiElement ? namedUsedApiElement.getSimpleName() : usedApiElement.toString(),
			source,
			use
		);
	}

	@Override
	public int hashCode() {
	    // CtElement::equals/hashCode() do not check the position
	    return Objects.hash(
            element.getPosition().toString(),
            element.toString(),
            usedApiElement.getPosition().toString(),
            source.getPosition().toString(),
            use,
            change);
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;
		if (that == null)
			return false;
		if (getClass() != that.getClass())
			return false;
		BrokenUse other = (BrokenUse) that;
		return comparePositions(element, other.element)
		    && comparePositions(usedApiElement, other.usedApiElement)
		    && comparePositions(source, other.source)
		    && use == other.use
		    && change == other.change;
	}

	/**
	 * Compares two Spoon positions based on their string representation.
	 *
	 * @param elem1 first Spoon element to compare
	 * @param elem2 second Spoon element to compare
	 * @return true if the positions of the elements are the same, false
	 *         otherwise
	 */
	private boolean comparePositions(CtElement elem1, CtElement elem2) {
	    return Objects.equals(elem1.getPosition().toString(),
	        elem2.getPosition().toString());
	}
}
