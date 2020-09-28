/*
 * Copyright 2020 Lukas Krejci
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.revapi.classland.impl.model.element;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

import static org.revapi.classland.impl.util.Memoized.memoize;
import static org.revapi.classland.impl.util.Memoized.obtained;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;

import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.GenericTypeParameters;
import org.revapi.classland.impl.model.signature.SignatureParser;
import org.revapi.classland.impl.model.signature.TypeParameterBound;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Modifiers;
import org.revapi.classland.impl.util.Nullable;

public final class TypeElementImpl extends TypeElementBase implements TypeVariableResolutionContext {
    private final String internalName;
    private final Memoized<NameImpl> qualifiedName;
    private final Memoized<NameImpl> simpleName;
    private final Memoized<ScanningResult> scan;
    private final Memoized<ElementImpl> enclosingElement;
    private final Memoized<NestingKind> nestingKind;
    private final Memoized<TypeMirrorImpl> superClass;
    private final Memoized<List<TypeMirrorImpl>> interfaces;
    private final Memoized<ElementKind> elementKind;
    private final Memoized<Set<Modifier>> modifiers;
    private final Memoized<List<ElementImpl>> enclosedElements;
    private final Memoized<Map<String, TypeParameterElementImpl>> typeParametersMap;
    private final Memoized<DeclaredTypeImpl> type;
    private final Memoized<List<TypeParameterElementImpl>> typeParameters;
    private final Memoized<Map<String, ExecutableElementImpl>> methods;
    private final Memoized<GenericTypeParameters> signature;

    public TypeElementImpl(Universe universe, String internalName, Memoized<ClassNode> node, PackageElementImpl pkg) {
        super(universe, internalName, obtained(pkg), node.map(AnnotationSource::fromType));
        this.internalName = internalName;

        this.scan = node.map(cls -> {
            ScanningResult ret = new ScanningResult();
            ret.classNode = cls;
            ret.nestingKind = NestingKind.TOP_LEVEL;
            ret.effectiveAccess = cls.access;

            if (cls.innerClasses.isEmpty()) {
                ret.simpleName = cls.name.replace('/', '.');
            } else {
                int classNameLength = cls.name.length();
                ret.qualifiedNameParts = new HashMap<>(cls.innerClasses.size(), 1);
                ret.innerClasses = new ArrayList<>();
                for (InnerClassNode icn : cls.innerClasses) {
                    // The list of the inner classes recorded on a type seems to contain all the containing classes +
                    // all the directly contained classes + the type itself. Therefore we can rely simply on the length
                    // of the name to distinguish between them.
                    if (icn.name.length() <= classNameLength) {
                        ret.qualifiedNameParts.put(icn.name, icn);

                        if (icn.name.length() == classNameLength) {
                            if (icn.innerName == null) {
                                ret.nestingKind = NestingKind.ANONYMOUS;
                                ret.outerClass = icn.name.substring(0, icn.name.lastIndexOf('$'));
                            } else if (icn.outerName == null) {
                                ret.nestingKind = NestingKind.LOCAL;
                            } else {
                                ret.nestingKind = NestingKind.MEMBER;
                                ret.outerClass = icn.outerName;
                            }
                            ret.simpleName = icn.innerName == null ? "" : icn.innerName;
                            ret.effectiveAccess = icn.access;
                        }
                    } else if (cls.name.equals(icn.outerName)) {
                        if (!Modifiers.isSynthetic(icn.access)) {
                            ret.innerClasses.add(icn.name);
                        }
                    }
                }

                if (ret.simpleName == null) {
                    // we're parsing a top-level class with inner classes
                    ret.simpleName = cls.name.replace('/', '.');
                }
            }

            return ret;
        });

        nestingKind = scan.map(r -> r.nestingKind);

        simpleName = scan.map(r -> {
            if (r.nestingKind == NestingKind.TOP_LEVEL) {
                return NameImpl.of(r.simpleName.substring(r.simpleName.lastIndexOf('.') + 1));
            }
            return NameImpl.of(r.simpleName);
        });

        qualifiedName = scan.map(r -> {
            if (r.nestingKind == NestingKind.TOP_LEVEL) {
                return NameImpl.of(r.simpleName);
            }

            if (r.nestingKind == NestingKind.LOCAL || r.nestingKind == NestingKind.ANONYMOUS) {
                return NameImpl.EMPTY;
            }

            String name = r.classNode.name;
            InnerClassNode icn = r.qualifiedNameParts.get(name);
            List<String> parts = new ArrayList<>(r.qualifiedNameParts.size());
            while (icn != null) {
                parts.add(0, icn.innerName);
                InnerClassNode next = r.qualifiedNameParts.get(icn.outerName);
                if (next == null) {
                    String outerClass = icn.outerName;
                    if (outerClass == null) {
                        // we're embedded in an anonymous class, let's bail...
                        return NameImpl.EMPTY;
                    } else {
                        parts.add(0, outerClass.replace('/', '.'));
                    }
                    break;
                } else {
                    icn = next;
                }
            }

            return NameImpl.of(String.join(".", parts));
        });

        elementKind = node.map(n -> Modifiers.toTypeElementKind(n.access));
        modifiers = scan.map(r -> Modifiers.toTypeModifiers(r.effectiveAccess));

        enclosingElement = scan.map(r -> {
            switch (r.nestingKind) {
            case TOP_LEVEL:
                return pkg;
            case MEMBER:
            case ANONYMOUS:
                return universe.getTypeByInternalNameFromPackage(r.outerClass, pkg);
            case LOCAL:
                return universe.getTypeByInternalNameFromPackage(r.classNode.outerClass, pkg)
                        .getMethod(r.classNode.outerMethod, r.classNode.outerMethodDesc);
            default:
                throw new IllegalStateException("Unhandled nesting kind, " + r.nestingKind
                        + ", while determining the enclosing element of class " + internalName);
            }

        });

        signature = scan.map(s -> {
            TypeElementBase outerClass = s.outerClass == null ? null
                    : universe.getTypeByInternalNameFromPackage(s.outerClass, pkg);

            ClassNode n = s.classNode;

            if (n.signature == null) {
                return new GenericTypeParameters(new LinkedHashMap<>(0, 0.01f),
                        new TypeSignature.Reference(0, n.superName, emptyList(), null), n.interfaces.stream()
                                .map(i -> new TypeSignature.Reference(0, i, emptyList(), null)).collect(toList()),
                        outerClass);
            } else {
                return SignatureParser.parseType(n.signature, outerClass);
            }
        });

        type = memoize(() -> TypeMirrorFactory.create(universe, this));

        superClass = signature.map(ts -> TypeMirrorFactory.create(universe, ts.superClass, this, asAnnotationSource(),
                new AnnotationTargetPath(TypeReference.newSuperTypeReference(-1)), obtained(pkg.getModule())));

        interfaces = signature.map(ts -> {
            List<TypeMirrorImpl> ret = new ArrayList<>(ts.interfaces.size());

            int i = 0;
            for (TypeSignature iface : ts.interfaces) {
                ret.add(TypeMirrorFactory.create(universe, iface, this, asAnnotationSource(),
                        new AnnotationTargetPath(TypeReference.newSuperTypeReference(i++)), obtained(pkg.getModule())));
            }

            return ret;
        });

        typeParametersMap = signature.map(ts -> {
            int i = 0;
            LinkedHashMap<String, TypeParameterElementImpl> typeParams = new LinkedHashMap<>();
            for (Map.Entry<String, TypeParameterBound> e : ts.typeParameters.entrySet()) {
                typeParams.put(e.getKey(), new TypeParameterElementImpl(universe, e.getKey(), this, e.getValue(), i++));
            }
            return typeParams;
        });

        typeParameters = typeParametersMap.map(m -> new ArrayList<>(m.values()));

        methods = node.map(n -> n.methods.stream().filter(m -> !Modifiers.isSynthetic(m.access))
                .collect(toMap(m -> m.name + "#" + m.desc, m -> new ExecutableElementImpl(universe, this, m))));

        enclosedElements = scan.map(r -> {
            Stream<VariableElementImpl.Field> fields = r.classNode.fields.stream()
                    .filter(f -> !Modifiers.isSynthetic(f.access))
                    .map(f -> new VariableElementImpl.Field(universe, this, f));

            Stream<TypeElementBase> innerClasses = r.innerClasses.stream()
                    .map(c -> universe.getTypeByInternalNameFromPackage(c, pkg));

            return concat(concat(fields, methods.get().values().stream()), innerClasses).collect(toList());
        });
    }

    public boolean isInPackage(PackageElementImpl pkg) {
        return getNestingKind() == NestingKind.TOP_LEVEL && this.pkg.get().equals(pkg);
    }

    public @Nullable ExecutableElementImpl getMethod(String methodName, String methodDescriptor) {
        return methods.get().get(methodName + "#" + methodDescriptor);
    }

    public Memoized<AnnotationSource> asAnnotationSource() {
        return scan.map(s -> AnnotationSource.fromType(s.classNode));
    }

    public Memoized<GenericTypeParameters> getSignature() {
        return signature;
    }

    @Override
    public Optional<TypeParameterElementImpl> resolveTypeVariable(String name) {
        TypeParameterElementImpl p = typeParametersMap.get().get(name);
        if (p == null && scan.get().outerClass != null) {
            TypeElementImpl outerClass = (TypeElementImpl) getEnclosingElement();
            return outerClass.resolveTypeVariable(name);
        } else {
            return Optional.ofNullable(p);
        }
    }

    @Override
    public NestingKind getNestingKind() {
        return nestingKind.get();
    }

    @Override
    public Name getQualifiedName() {
        return qualifiedName.get();
    }

    @Override
    public TypeMirrorImpl getSuperclass() {
        return superClass.get();
    }

    @Override
    public List<TypeMirrorImpl> getInterfaces() {
        return interfaces.get();
    }

    @Override
    public List<TypeParameterElementImpl> getTypeParameters() {
        return typeParameters.get();
    }

    @Override
    public DeclaredTypeImpl asType() {
        return type.get();
    }

    @Override
    public ElementKind getKind() {
        return elementKind.get();
    }

    @Override
    public Set<Modifier> getModifiers() {
        return modifiers.get();
    }

    @Override
    public Name getSimpleName() {
        return simpleName.get();
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return enclosingElement.get();
    }

    @Override
    public List<ElementImpl> getEnclosedElements() {
        return enclosedElements.get();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitType(this, p);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TypeElementImpl that = (TypeElementImpl) o;
        return internalName.equals(that.internalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalName);
    }

    @Override
    public String toString() {
        return "TypeElementImpl{" + internalName + "}";
    }

    private static final class ScanningResult {
        ClassNode classNode;
        int effectiveAccess;
        NestingKind nestingKind;
        String outerClass;
        String simpleName;
        Map<String, InnerClassNode> qualifiedNameParts;
        List<String> innerClasses;
    }
}
