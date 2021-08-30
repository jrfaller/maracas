package org.swat.maracas.spoon.delta;

import japicmp.model.JApiCompatibilityChange;
import spoon.reflect.declaration.CtElement;

public abstract class AbstractBrokenDeclaration implements BrokenDeclaration {
	protected final JApiCompatibilityChange change;
	protected CtElement sourceElement;

	public AbstractBrokenDeclaration(JApiCompatibilityChange change) {
		this.change = change;
	}

	@Override
	public JApiCompatibilityChange getChange() {
		return change;
	}

	@Override
	public CtElement getSourceElement() {
		return this.sourceElement;
	}

	@Override
	public void setSourceElement(CtElement element) {
		this.sourceElement = element;
	}
}
