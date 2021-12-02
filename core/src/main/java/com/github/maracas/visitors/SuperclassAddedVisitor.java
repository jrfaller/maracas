package com.github.maracas.visitors;

import java.util.Arrays;
import java.util.HashSet;

import japicmp.model.JApiCompatibilityChange;
import spoon.reflect.reference.CtTypeReference;

public class SuperclassAddedVisitor extends SupertypeAddedVisitor {

	public SuperclassAddedVisitor(CtTypeReference<?> clsRef, CtTypeReference<?> superRef) {
		super(clsRef, new HashSet<CtTypeReference<?>>(Arrays.asList(superRef)), JApiCompatibilityChange.SUPERCLASS_ADDED);
	}
}
