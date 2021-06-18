package org.swat.maracas.spoon.visitors;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.swat.maracas.spoon.Detection;

import japicmp.model.JApiAnnotation;
import japicmp.model.JApiClass;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiSuperclass;
import japicmp.output.Filter.FilterVisitor;
import javassist.CtField;
import javassist.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

public class ImpactVisitor implements FilterVisitor {
	private final CtPackage root;
	private final Set<Detection> detections = new HashSet<>();

	public ImpactVisitor(CtPackage root) {
		this.root = root;
	}

	public Set<Detection> getDetections() {
		return detections;
	}

	public boolean matchingSignatures(CtExecutableReference<?> spoonMethod, CtMethod japiMethod) {
		return
			japiMethod.getName().concat(japiMethod.getSignature()).startsWith(spoonMethod.getSignature());
	}

	@Override
	public void visit(Iterator<JApiClass> iterator, JApiClass elem) {
		CtTypeReference<?> clsRef = root.getFactory().Type().createReference(elem.getFullyQualifiedName());
		elem.getCompatibilityChanges().forEach(c -> {
			BreakingChangeVisitor visitor = switch (c) {
				case CLASS_NO_LONGER_PUBLIC -> null; // CLASS_LESS_ACCESSIBLE is a superset; fix japicmp 
				case CLASS_LESS_ACCESSIBLE -> new ClassLessAccessibleVisitor(clsRef, elem.getAccessModifier().getNewModifier().get());
				case CLASS_NOW_ABSTRACT -> new ClassNowAbstractVisitor(clsRef);
				case CLASS_NOW_FINAL -> new ClassNowFinalVisitor(clsRef);
				case ANNOTATION_DEPRECATED_ADDED -> new AnnotationDeprecatedAddedVisitor(clsRef);
				default -> null;
			};
			
			if (visitor != null) {
				visitor.scan(root);
				detections.addAll(visitor.getDetections());
			}
		});
	}

	@Override
	public void visit(Iterator<JApiMethod> iterator, JApiMethod elem) {
		CtTypeReference<?> clsRef = root.getFactory().Type().createReference(elem.getjApiClass().getFullyQualifiedName());
		elem.getCompatibilityChanges().forEach(c -> {
			japicmp.util.Optional<CtMethod> oldMethodOpt = elem.getOldMethod();
			if (oldMethodOpt.isPresent()) {
				CtMethod oldMethod = oldMethodOpt.get();
				
				Optional<CtExecutableReference<?>> mRefOpt =
					clsRef.getDeclaredExecutables()
					.stream()
					.filter(m -> matchingSignatures(m, oldMethod))
					.findFirst();

				if (mRefOpt.isPresent()) {
					BreakingChangeVisitor visitor = switch (c) {
						case METHOD_NOW_FINAL -> new MethodNowFinalVisitor(mRefOpt.get());
						default -> null;
					};
					
					if (visitor != null) {
						visitor.scan(root);
						detections.addAll(visitor.getDetections());
					}
				} else {
					if (oldMethod.getName().equals("values") || oldMethod.getName().equals("valueOf"))
						// When an enum is transformed into anything else,
						// japicmp reports that valueOf(String)/values() are removed
						// Ignore.
						;
					else
						throw new RuntimeException("Unmatched " + oldMethod);
				}
			} else {
				// No oldMethod
			}
		});
	}

	@Override
	public void visit(Iterator<JApiConstructor> iterator, JApiConstructor elem) {
	}

	@Override
	public void visit(Iterator<JApiImplementedInterface> iterator, JApiImplementedInterface elem) {
	}

	@Override
	public void visit(Iterator<JApiField> iterator, JApiField elem) {
		CtTypeReference<?> clsRef = root.getFactory().Type().createReference(elem.getjApiClass().getFullyQualifiedName());
		elem.getCompatibilityChanges().forEach(c -> {
			japicmp.util.Optional<CtField> oldFieldOpt = elem.getOldFieldOptional();
			if (oldFieldOpt.isPresent()) {
				CtField oldField = oldFieldOpt.get();
				CtFieldReference<?> fRef = clsRef.getDeclaredField(oldField.getName());

				BreakingChangeVisitor visitor = switch (c) {
					case FIELD_NOW_FINAL -> new FieldNowFinalVisitor(fRef);
					default -> null;
				};
				
				if (visitor != null) {
					visitor.scan(root);
					detections.addAll(visitor.getDetections());
				}
			} else {
				// No oldMethod
			}
		});
	}

	@Override
	public void visit(Iterator<JApiAnnotation> iterator, JApiAnnotation elem) {
	}

	@Override
	public void visit(JApiSuperclass elem) {
	}
}
