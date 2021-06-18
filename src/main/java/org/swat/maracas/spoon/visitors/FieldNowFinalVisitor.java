package org.swat.maracas.spoon.visitors;

import org.swat.maracas.spoon.Detection.APIUse;

import japicmp.model.JApiCompatibilityChange;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.reference.CtFieldReference;

/**
 * Detections of METHOD_NOW_FINAL are:
 *	- Attempting to write-access a now-final field
 */
public class FieldNowFinalVisitor extends BreakingChangeVisitor {
	private final CtFieldReference<?> fRef;

	protected FieldNowFinalVisitor(CtFieldReference<?> fRef) {
		super(JApiCompatibilityChange.FIELD_NOW_FINAL);
		this.fRef = fRef;
	}

	@Override
		public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite) {
			if (fRef.equals(fieldWrite.getVariable()))
				detection(fieldWrite, fieldWrite.getVariable(), fRef, APIUse.FIELD_ACCESS);

			super.visitCtFieldWrite(fieldWrite);
		}
}
