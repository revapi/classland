/*
 * Copyright 2020-2022 Lukas Krejci
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
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

import static org.revapi.classland.impl.util.Asm.hasFlag;
import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;
import static org.revapi.classland.impl.util.MemoizedValue.obtainedEmptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.type.TypeKind;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.*;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Modifiers;
import org.revapi.classland.impl.util.Nullable;

public final class TypeElementImpl extends TypeElementBase {
    private final MemoizedValue<ClassNode> node;
    private final MemoizedValue<NameImpl> qualifiedName;
    private final MemoizedValue<NameImpl> simpleName;
    private final MemoizedValue<ScanningResult> scan;
    private final MemoizedValue<ElementImpl> enclosingElement;
    private final MemoizedValue<NestingKind> nestingKind;
    private final MemoizedValue<TypeMirrorImpl> superClass;
    private final MemoizedValue<List<TypeMirrorImpl>> interfaces;
    private final MemoizedValue<ElementKind> elementKind;
    private final MemoizedValue<Set<Modifier>> modifiers;
    private final MemoizedValue<List<ElementImpl>> enclosedElements;
    private final MemoizedValue<Map<String, TypeParameterElementImpl>> typeParametersMap;
    private final MemoizedValue<DeclaredTypeImpl> type;
    private final MemoizedValue<List<TypeParameterElementImpl>> typeParameters;
    private final MemoizedValue<Map<String, ExecutableElementImpl>> methods;
    private final MemoizedValue<Map<String, VariableElementImpl.Field>> fields;
    private final MemoizedValue<GenericTypeParameters> signature;

    public TypeElementImpl(TypeLookup lookup, @Nullable Archive archive, String internalName,
            MemoizedValue<ClassNode> node, PackageElementImpl pkg) {
        super(lookup, archive, internalName, obtained(pkg), node.map(AnnotationSource::fromType));
        this.node = node;

        this.scan = node.map(cls -> {
            ScanningResult ret = new ScanningResult();
            ret.classNode = cls;
            ret.nestingKind = NestingKind.TOP_LEVEL;
            ret.effectiveAccess = cls.access;

            ret.outerClass = cls.outerClass;

            if (cls.innerClasses.isEmpty()) {
                ret.simpleName = cls.name.replace('/', '.');
            } else {
                int classNameLength = cls.name.length();
                ret.qualifiedNameParts = new HashMap<>(cls.innerClasses.size(), 1);
                ret.innerClasses = new ArrayList<>();
                for (InnerClassNode icn : cls.innerClasses) {
                    // The list of the inner classes recorded on a type seems to contain all the containing classes +
                    // all the directly contained classes + the type itself. Therefore, we can rely simply on the length
                    // of the name to distinguish between them.
                    // Note that the list of inner classes also contains inner classes that are not defined in this
                    // class (possibly they are just used in the class code), so we need to guard for that, too.
                    if (icn.name.length() <= classNameLength && cls.name.startsWith(icn.name)) {
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
                if (r.classNode.outerMethod == null) {
                    return lookup.getTypeByInternalNameFromPackage(r.outerClass, pkg);
                } else {
                    return lookup.getTypeByInternalNameFromPackage(r.classNode.outerClass, pkg)
                            .getMethod(r.classNode.outerMethod, r.classNode.outerMethodDesc);
                }
            case LOCAL:
                return lookup.getTypeByInternalNameFromPackage(r.classNode.outerClass, pkg)
                        .getMethod(r.classNode.outerMethod, r.classNode.outerMethodDesc);
            default:
                throw new IllegalStateException("Unhandled nesting kind, " + r.nestingKind
                        + ", while determining the enclosing element of class " + internalName);
            }

        });

        signature = scan.map(s -> {
            TypeElementBase outerClass = s.outerClass == null ? null
                    : lookup.getTypeByInternalNameFromPackage(s.outerClass, pkg);

            ClassNode n = s.classNode;

            if (n.signature == null) {
                boolean noSuperClass = n.superName == null || elementKind.get() == ElementKind.INTERFACE
                        || elementKind.get() == ElementKind.ANNOTATION_TYPE;

                return new GenericTypeParameters(new LinkedHashMap<>(0, 0.01f),
                        noSuperClass ? null : new TypeSignature.Reference(0, n.superName, emptyList(), null),
                        n.interfaces.stream().map(i -> new TypeSignature.Reference(0, i, emptyList(), null))
                                .collect(toList()),
                        outerClass);
            } else {
                return SignatureParser.parseType(n.signature, outerClass);
            }
        });

        type = memoize(() -> TypeMirrorFactory.create(lookup, this));

        superClass = signature.map(ts -> {
            if (ts.superClass == null) {
                // java.lang.Object or interfaces
                return new NoTypeImpl(lookup, obtainedEmptyList(), TypeKind.NONE);
            } else {
                return TypeMirrorFactory.create(lookup, ts.superClass, this, asAnnotationSource(),
                        new AnnotationTargetPath(TypeReference.newSuperTypeReference(-1)), obtained(pkg.getModule()));
            }
        });

        interfaces = signature.map(ts -> {
            List<TypeMirrorImpl> ret = new ArrayList<>(ts.interfaces.size());

            int i = 0;
            for (TypeSignature iface : ts.interfaces) {
                ret.add(TypeMirrorFactory.create(lookup, iface, this, asAnnotationSource(),
                        new AnnotationTargetPath(TypeReference.newSuperTypeReference(i++)), obtained(pkg.getModule())));
            }

            return ret;
        });

        typeParametersMap = signature.map(ts -> {
            int i = 0;
            LinkedHashMap<String, TypeParameterElementImpl> typeParams = new LinkedHashMap<>();
            for (Map.Entry<String, TypeParameterBound> e : ts.typeParameters.entrySet()) {
                typeParams.put(e.getKey(), new TypeParameterElementImpl(lookup, e.getKey(), this, e.getValue(), i++));
            }
            return typeParams;
        });

        typeParameters = typeParametersMap.map(m -> new ArrayList<>(m.values()));

        methods = node.map(n -> n.methods.stream().filter(m -> !Modifiers.isSynthetic(m.access))
                .collect(toMap(m -> m.name + "#" + m.desc, m -> new ExecutableElementImpl(lookup, this, m))));

        fields = node.map(n -> n.fields.stream().filter(f -> !Modifiers.isSynthetic(f.access))
                .map(f -> new VariableElementImpl.Field(lookup, this, f))
                .collect(Collectors.toMap(v -> v.getSimpleName().asString(), identity())));

        enclosedElements = scan.map(r -> {
            Stream<TypeElementBase> innerClasses = r.innerClasses == null ? Stream.empty()
                    : r.innerClasses.stream().map(c -> lookup.getTypeByInternalNameFromPackage(c, pkg));

            return concat(concat(fields.get().values().stream(), methods.get().values().stream()), innerClasses)
                    .collect(toList());
        });
    }

    @Override
    public Archive getArchive() {
        return super.getArchive();
    }

    @Override
    public boolean isDeprecated() {
        return hasFlag(node.get().access, Opcodes.ACC_DEPRECATED) || isAnnotatedDeprecated();
    }

    public MemoizedValue<ClassNode> getNode() {
        return node;
    }

    public MemoizedValue<DeclaredTypeImpl> getType() {
        return type;
    }

    public @Nullable ExecutableElementImpl getMethod(String methodName, String methodDescriptor) {
        return methods.get().get(methodName + "#" + methodDescriptor);
    }

    @Override
    public List<ExecutableElementImpl> getMethod(String methodName) {
        String methodKey = methodName + "#";
        return methods.get().entrySet().stream().filter(e -> e.getKey().startsWith(methodKey)).map(Map.Entry::getValue)
                .collect(toList());
    }

    @Override
    public VariableElementImpl.@Nullable Field getField(String name) {
        return fields.get().get(name);
    }

    public MemoizedValue<AnnotationSource> asAnnotationSource() {
        return scan.map(s -> AnnotationSource.fromType(s.classNode));
    }

    public MemoizedValue<GenericTypeParameters> getSignature() {
        return signature;
    }

    @Override
    public ElementImpl asElement() {
        return this;
    }

    @Override
    public Optional<TypeParameterElementImpl> resolveTypeVariable(String name) {
        TypeParameterElementImpl p = typeParametersMap.get().get(name);
        if (p == null) {
            ElementImpl outer = getEnclosingElement();
            if (outer instanceof TypeVariableResolutionContext) {
                return ((TypeVariableResolutionContext) outer).resolveTypeVariable(name);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(p);
        }
    }

    @Override
    public NestingKind getNestingKind() {
        return nestingKind.get();
    }

    @Override
    public NameImpl getQualifiedName() {
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
    public NameImpl getSimpleName() {
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
