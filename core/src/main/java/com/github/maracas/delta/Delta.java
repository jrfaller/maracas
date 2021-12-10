package com.github.maracas.delta;

import com.github.maracas.util.PathHelpers;
import com.github.maracas.util.SpoonHelpers;
import com.github.maracas.visitors.BreakingChangeVisitor;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiClass;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiSuperclass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import spoon.SpoonException;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A delta model lists the breaking changes between two versions of a library,
 * represented as a collection of {@link BrokenDeclaration}.
 */
public class Delta {
    /**
     * The library's old JAR
     */
    private final Path oldJar;
    /**
     * The library's new JAR
     */
    private final Path newJar;
    /**
     * The list of {@link BrokenDeclaration} extracted from japicmp's classes
     */
    private final Collection<BrokenDeclaration> brokenDeclarations;

    /**
     * @see #fromJApiCmpDelta(Path, Path, List)
     */
    private Delta(Path oldJar, Path newJar, Collection<BrokenDeclaration> decls) {
        this.oldJar = oldJar;
        this.newJar = newJar;
        this.brokenDeclarations = decls;
    }

    /**
     * Builds a delta model from the list of changes extracted by japicmp
     *
     * @param oldJar the library's old JAR
     * @param newJar the library's new JAR
     * @param classes the list of changes extracted using
     *        {@link japicmp.cmp.JarArchiveComparator#compare(japicmp.cmp.JApiCmpArchive, japicmp.cmp.JApiCmpArchive)}
     * @return a corresponding new delta model
     */
    public static Delta fromJApiCmpDelta(Path oldJar, Path newJar, List<JApiClass> classes) {
        Objects.requireNonNull(oldJar);
        Objects.requireNonNull(newJar);
        Objects.requireNonNull(classes);

        Collection<BrokenDeclaration> brokenDeclarations = new ArrayList<>();

        // We need to create CtReferences to v1 to map japicmp's delta
        // to our own. Building an empty model with the right
        // classpath allows us to create these references.
        CtModel model = SpoonHelpers.buildSpoonModel(null, oldJar);
        CtPackage root = model.getRootPackage();

        JApiCmpDeltaVisitor.visit(classes, new JApiCmpDeltaVisitor() {
            @Override
            public void visit(JApiClass cls) {
                CtTypeReference<?> clsRef = root.getFactory().Type().createReference(cls.getFullyQualifiedName());

                cls.getCompatibilityChanges().forEach(c ->
                    brokenDeclarations.add(new BrokenClass(cls, clsRef, c))
                );

                cls.getInterfaces().forEach(i ->
                    visit(cls, i)
                );
            }

            @Override
            public void visit(JApiMethod m) {
                var oldMethodOpt = m.getOldMethod();
                var newMethodOpt = m.getNewMethod();
                CtTypeReference<?> clsRef = root.getFactory().Type().createReference(m.getjApiClass().getFullyQualifiedName());

                // A breaking change affecting an existing method: we can resolve it
                if (oldMethodOpt.isPresent()) {
                    CtMethod oldMethod = oldMethodOpt.get();
                    String sign = SpoonHelpers.buildSpoonSignature(m);
                    CtExecutableReference<?> mRef = root.getFactory().Method().createReference(sign);

                    try {
                        if (mRef.getExecutableDeclaration() != null) {
                            m.getCompatibilityChanges().forEach(c ->
                              brokenDeclarations.add(new BrokenMethod(m, mRef, c))
                            );
                        } else {
                            if (oldMethod.getName().equals("values") || oldMethod.getName().equals("valueOf")) {
                                // When an enum is transformed into anything else,
                                // japicmp reports that valueOf(String)/values() are removed
                                // Ignore. FIXME
                                ;
                            } else {
                                System.err.println("Warning: Couldn't resolve method %s in the Spoon model".formatted(oldMethod));
                                System.err.println(clsRef.getDeclaredExecutables().stream().map(e -> e.getSignature()).toList());
                            }
                        }
                    } catch (SpoonException e) {
                        System.err.println("Couldn't find a Spoon reference for %s: %s".formatted(m, e.getMessage()));
                    }
                }
                // A breaking change due to a newly inserted method: we cannot resolve it
                else if (newMethodOpt.isPresent()) {
                    CtMethod newMethod = newMethodOpt.get();

                    // FIXME: we miss the information about the newly added method
                    if (!(newMethod.getName().equals("values") || newMethod.getName().equals("valueOf"))) {
                        m.getCompatibilityChanges().forEach(c ->
                            brokenDeclarations.add(new BrokenClass(m.getjApiClass(), clsRef, c))
                        );
                    }
                } else {
                    throw new RuntimeException("The JApiCmp delta model is corrupted.");
                }
            }

            @Override
            public void visit(JApiField f) {
                CtTypeReference<?> clsRef = root.getFactory().Type().createReference(f.getjApiClass().getFullyQualifiedName());
                var oldFieldOpt = f.getOldFieldOptional();
                if (oldFieldOpt.isPresent()) {
                    CtField oldField = oldFieldOpt.get();
                    CtFieldReference<?> fRef = clsRef.getDeclaredField(oldField.getName());

                    f.getCompatibilityChanges().forEach(c ->
                        brokenDeclarations.add(new BrokenField(f, fRef, c))
                    );
                } else {
                    // No oldField
                }
            }

            @Override
            public void visit(JApiConstructor cons) {
                CtTypeReference<?> clsRef = root.getFactory().Type().createReference(cons.getjApiClass().getFullyQualifiedName());
                var oldConsOpt = cons.getOldConstructor();

                if (oldConsOpt.isPresent()) {
                    CtConstructor oldCons = oldConsOpt.get();
                    String sign = SpoonHelpers.buildSpoonSignature(cons);
                    CtExecutableReference<?> consRef = root.getFactory().Constructor().createReference(sign);

                    // TODO: report bug in Spoon. Implicit constructor states that
                    // the opposite when calling isImplicit() method. Using getPosition()
                    // isValid() instead.
                    if (consRef.getExecutableDeclaration() != null) {
                        cons.getCompatibilityChanges().forEach(c ->
                            brokenDeclarations.add(new BrokenMethod(cons, consRef, c))
                        );
                    } else {
                        // No old constructor
                        System.err.println("Warning: Couldn't resolve constructor %s in the Spoon model".formatted(oldCons));
                        System.err.println(clsRef.getDeclaredExecutables().stream()
                          .filter(CtExecutableReference::isConstructor).map(e -> e.getSignature()).toList());
                    }
                }
            }

            @Override
            public void visit(JApiImplementedInterface intf) {
                // Using visit(JApiClass jApiClass, JApiImplementedInterface jApiImplementedInterface)
                // FIXME: is there a way to get the JApiClass from the interface?
            }

            @Override
            public void visit(JApiAnnotation ann) {
            }

            @Override
            public void visit(JApiSuperclass superCls) {
                JApiClass jApiClass = superCls.getJApiClassOwning();
                CtTypeReference<?> clsRef = root.getFactory().Type().createReference(jApiClass.getFullyQualifiedName());
                superCls.getCompatibilityChanges().forEach(c ->
                    brokenDeclarations.add(new BrokenClass(jApiClass, clsRef, c))
                );
            }

            public void visit(JApiClass jApiClass, JApiImplementedInterface jApiImplementedInterface) {
                CtTypeReference<?> clsRef = root.getFactory().Type().createReference(jApiClass.getFullyQualifiedName());
                jApiImplementedInterface.getCompatibilityChanges().forEach(c ->
                    brokenDeclarations.add(new BrokenClass(jApiClass, clsRef, c))
                );
            }
        });

        return new Delta(oldJar, newJar, brokenDeclarations);
    }

    /**
     * Delta models do not natively include source code locations. Invoking
     * this method with the old library's source code populates the source code
     * location for every breaking change.
     *
     * @param sources a {@link Path} to the old library's source code
     */
    public void populateLocations(Path sources) {
        if (!PathHelpers.isValidDirectory(sources))
            throw new IllegalArgumentException("sources isn't a valid directory");

        CtModel model = SpoonHelpers.buildSpoonModel(sources, null);
        CtPackage root = model.getRootPackage();

        brokenDeclarations.forEach(decl -> {
            CtReference bytecodeRef = decl.getReference();
            if (bytecodeRef instanceof CtTypeReference<?> typeRef && typeRef.getTypeDeclaration() != null) {
                CtTypeReference<?> sourceRef = root.getFactory().Type().createReference(typeRef.getTypeDeclaration());
                decl.setSourceElement(sourceRef.getTypeDeclaration());
            } else if (bytecodeRef instanceof CtExecutableReference<?> execRef && execRef.getExecutableDeclaration() != null) {
                CtExecutableReference<?> sourceRef = root.getFactory().Executable().createReference(execRef.getExecutableDeclaration());
                decl.setSourceElement(sourceRef.getExecutableDeclaration());
            } else if (bytecodeRef instanceof CtFieldReference<?> fieldRef && fieldRef.getFieldDeclaration() != null) {
                CtFieldReference<?> sourceRef = root.getFactory().Field().createReference(fieldRef.getFieldDeclaration());
                decl.setSourceElement(sourceRef.getFieldDeclaration());
            }
        });
    }

    /**
     * Returns a list of {@link BreakingChangeVisitor}, one per {@link BrokenDeclaration}
     * in the current delta model. Each visitor is responsible for inferring
     * the set of detections corresponding to the breaking change in client code.
     */
    public Collection<BreakingChangeVisitor> getVisitors() {
        return
            brokenDeclarations.stream()
            .map(BrokenDeclaration::getVisitor)
            .filter(Objects::nonNull) // Temporary; FIXME
            .toList();
    }

    /**
     * Returns the list of {@link BrokenDeclaration in the current delta model
     */
    public Collection<BrokenDeclaration> getBrokenDeclarations() {
        return brokenDeclarations;
    }

    /**
     * Returns the {@link Path} to the library's old JAR of the current delta
     */
    public Path getOldJar() {
        return oldJar;
    }

    /**
     * Returns the {@link Path} to the library's new JAR of the current delta
     */
    public Path getNewJar() {
        return newJar;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Δ(%s -> %s)\n".formatted(oldJar.getFileName(), newJar.getFileName()));
        sb.append(
            brokenDeclarations.stream()
            .map(bd -> """
                [%s]
                Reference: %s
                Source: %s %s
                """.formatted(
                    bd.getChange(),
                    bd.getReference(),
                    bd.getSourceElement() instanceof CtNamedElement ne ? ne.getSimpleName() : bd.getSourceElement(),
                    bd.getSourceElement() != null ? bd.getSourceElement().getPosition() : "<no source>")
                ).collect(Collectors.joining())
            );
        return sb.toString();
    }
}
