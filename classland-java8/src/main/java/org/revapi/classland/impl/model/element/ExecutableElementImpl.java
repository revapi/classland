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
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static org.objectweb.asm.TypeReference.METHOD_RETURN;
import static org.objectweb.asm.TypeReference.newTypeReference;
import static org.revapi.classland.impl.model.mirror.AnnotationValueImpl.fromAsmValue;
import static org.revapi.classland.impl.util.Asm.hasFlag;
import static org.revapi.classland.impl.util.Memoized.memoize;
import static org.revapi.classland.impl.util.Memoized.obtained;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.SimpleElementVisitor8;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.AnnotationValueImpl;
import org.revapi.classland.impl.model.mirror.ExecutableTypeImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.GenericMethodParameters;
import org.revapi.classland.impl.model.signature.SignatureParser;
import org.revapi.classland.impl.model.signature.TypeParameterBound;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Modifiers;
import org.revapi.classland.impl.util.Nullable;

public final class ExecutableElementImpl extends ExecutableElementBase {
    private final TypeElementImpl parent;
    private final MethodNode method;
    private final NameImpl name;
    private final Memoized<GenericMethodParameters> signature;
    private final Memoized<TypeMirrorImpl> returnType;
    private final Memoized<List<VariableElementImpl>> parameters;
    private final Memoized<ElementKind> elementKind;
    private final Memoized<Set<Modifier>> modifiers;
    private final Memoized<Map<String, TypeParameterElementImpl>> typeParameterMap;
    private final Memoized<List<TypeParameterElementImpl>> typeParameters;
    private final Memoized<? extends TypeMirrorImpl> receiverType;
    private final Memoized<List<TypeMirrorImpl>> thrownTypes;
    private final Memoized<TypeMirrorImpl> type;
    private final @Nullable Memoized<AnnotationValueImpl> defaultValue;

    public ExecutableElementImpl(Universe universe, TypeElementImpl parent, MethodNode method) {
        super(universe, obtained(AnnotationSource.fromMethod(method)), AnnotationTargetPath.ROOT,
                parent.lookupModule());
        this.parent = parent;
        this.method = method;
        this.name = NameImpl.of(method.name);
        Type methodType = Type.getMethodType(method.desc);
        Type[] parameterTypes = methodType.getArgumentTypes();

        this.signature = memoize(() -> {
            if (method.signature == null) {
                return new GenericMethodParameters(new LinkedHashMap<>(0, 0.01f),
                        SignatureParser.parseTypeRef(methodType.getReturnType().getInternalName()),
                        Stream.of(methodType.getArgumentTypes())
                                .map(t -> SignatureParser.parseTypeRef(t.getDescriptor())).collect(toList()),
                        method.exceptions.stream().map(SignatureParser::parseInternalName).collect(toList()), parent);
            } else {
                return SignatureParser.parseMethod(method.signature, parent);
            }
        });

        AnnotationSource annotationSource = AnnotationSource.fromMethod(method);

        this.returnType = signature
                .map(s -> TypeMirrorFactory.create(universe, s.returnType, this, obtained(annotationSource),
                        new AnnotationTargetPath(newTypeReference(METHOD_RETURN)), parent.lookupModule()));

        this.receiverType = parent.getNode().map(cls -> {
            boolean isStaticMethod = hasFlag(method.access, Opcodes.ACC_STATIC);

            if (isStaticMethod) {
                return new NoTypeImpl(universe, obtained(emptyList()), TypeKind.NONE);
            }

            boolean isStaticClass = hasFlag(cls.access, Opcodes.ACC_STATIC)
                    || parent.getInternalName().indexOf('$') == -1 || parent.getNestingKind() == NestingKind.TOP_LEVEL;

            if ("<init>".equals(method.name)) {
                if (isStaticClass) {
                    return new NoTypeImpl(universe, obtained(emptyList()), TypeKind.NONE);
                } else {
                    return parent.getEnclosingElement().accept(new SimpleElementVisitor8<TypeMirrorImpl, Void>() {
                        @Override
                        protected TypeMirrorImpl defaultAction(Element e, Void aVoid) {
                            return new NoTypeImpl(universe, obtained(emptyList()), TypeKind.NONE);
                        }

                        @Override
                        public TypeMirrorImpl visitType(TypeElement e, Void aVoid) {
                            if (parameterTypes[0].getInternalName().equals(((TypeElementImpl) e).getInternalName())) {
                                return TypeMirrorFactory.create(universe, signature.get().parameterTypes.get(0),
                                        ExecutableElementImpl.this, obtained(annotationSource),
                                        new AnnotationTargetPath(TypeReference.newFormalParameterReference(0)),
                                        parent.lookupModule());
                            } else {
                                return new NoTypeImpl(universe, obtained(emptyList()), TypeKind.NONE);
                            }
                        }
                    }, null);
                }
            } else {
                boolean hasAnnotatedReceiverParam = (method.visibleAnnotableParameterCount > 0
                        && method.visibleTypeAnnotations != null
                        && method.visibleAnnotableParameterCount < method.visibleTypeAnnotations.size())
                        || (method.visibleAnnotableParameterCount > 0 && method.invisibleTypeAnnotations != null
                                && method.visibleAnnotableParameterCount < method.invisibleTypeAnnotations.size());

                if (hasAnnotatedReceiverParam) {
                    return TypeMirrorFactory.create(universe,
                            SignatureParser.parseInternalName(parent.getInternalName()), this,
                            obtained(annotationSource),
                            new AnnotationTargetPath(TypeReference.newFormalParameterReference(0)),
                            parent.lookupModule());
                } else {
                    return TypeMirrorFactory.create(universe, parent, emptyList(), emptyList());
                }
            }
        });

        this.parameters = parameterTypes.length == 0 ? obtained(emptyList()) : receiverType.map(receiver -> {
            int paramShift;
            if ("<init>".equals(method.name)) {
                // we need to look out for the synthetic parameter of the instance inner class constructors that
                // is being passed the "this" out their outer class.
                // we try to avoid determining the nesting kind of the parent, because that requires the parent
                // parsing.
                paramShift = receiver.getKind() == TypeKind.NONE ? 0 : 1;
            } else {
                paramShift = 0;
            }

            List<VariableElementImpl> ret = new ArrayList<>(parameterTypes.length - paramShift);
            for (int i = paramShift; i < parameterTypes.length; ++i) {
                ret.add(new VariableElementImpl.Parameter(universe, this, i));
            }
            return ret;
        });

        this.elementKind = memoize(() -> {
            if ("<init>".equals(method.name)) {
                return ElementKind.CONSTRUCTOR;
            } else if ("<clinit>".equals(method.name)) {
                return ElementKind.STATIC_INIT;
            } else if (method.name == null || "".equals(method.name)) {
                if ((method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
                    return ElementKind.STATIC_INIT;
                } else {
                    return ElementKind.INSTANCE_INIT;
                }
            } else {
                return ElementKind.METHOD;
            }
        });

        this.modifiers = memoize(() -> Modifiers.toMethodModifiers(method.access));

        this.typeParameterMap = signature.map(mp -> {
            int i = 0;
            LinkedHashMap<String, TypeParameterElementImpl> typeParams = new LinkedHashMap<>();
            for (Map.Entry<String, TypeParameterBound> e : mp.typeParameters.entrySet()) {
                typeParams.put(e.getKey(), new TypeParameterElementImpl(universe, e.getKey(), this, e.getValue(), i++));
            }
            return typeParams;
        });

        this.typeParameters = typeParameterMap.map(m -> new ArrayList<>(m.values()));

        this.thrownTypes = signature.map(sig -> {
            int i = 0;
            List<TypeMirrorImpl> ret = new ArrayList<>(sig.exceptionTypes.size());

            for(TypeSignature ex : sig.exceptionTypes) {
                ret.add(TypeMirrorFactory.create(universe, ex, this, obtained(annotationSource),
                        new AnnotationTargetPath(TypeReference.newExceptionReference(i++)), parent.lookupModule()));
            }

            return ret;
        });

        this.type = memoize(() -> new ExecutableTypeImpl(this));

        this.defaultValue = method.annotationDefault == null
                ? null
                : memoize(() -> fromAsmValue(universe, method.annotationDefault, this, parent.lookupModule()));
    }

    MethodNode getNode() {
        return method;
    }

    Memoized<GenericMethodParameters> getSignature() {
        return signature;
    }

    public TypeElementImpl getType() {
        return parent;
    }

    @Override
    public Optional<TypeParameterElementImpl> resolveTypeVariable(String name) {
        TypeParameterElementImpl typeParam = typeParameterMap.get().get(name);
        return typeParam == null ? parent.resolveTypeVariable(name) : Optional.of(typeParam);
    }

    @Override
    public Memoized<AnnotationSource> asAnnotationSource() {
        return obtained(AnnotationSource.fromMethod(method));
    }

    @Override
    public Memoized<ModuleElementImpl> lookupModule() {
        return parent.lookupModule();
    }

    @Override
    public ElementImpl asElement() {
        return this;
    }

    @Override
    public List<TypeParameterElementImpl> getTypeParameters() {
        return typeParameters.get();
    }

    @Override
    public TypeMirrorImpl getReturnType() {
        return returnType.get();
    }

    @Override
    public List<VariableElementImpl> getParameters() {
        return parameters.get();
    }

    @Override
    public TypeMirrorImpl getReceiverType() {
        return receiverType.get();
    }

    @Override
    public boolean isVarArgs() {
        return (method.access & Opcodes.ACC_VARARGS) == Opcodes.ACC_VARARGS;
    }

    @Override
    public boolean isDefault() {
        return (method.access & Opcodes.ACC_ABSTRACT) != Opcodes.ACC_ABSTRACT
                && parent.getKind() == ElementKind.INTERFACE;
    }

    @Override
    public List<TypeMirrorImpl> getThrownTypes() {
        return thrownTypes.get();
    }

    @Override
    public AnnotationValueImpl getDefaultValue() {
        return defaultValue == null ? null : defaultValue.get();
    }

    @Override
    public TypeMirrorImpl asType() {
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
        return name;
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return parent;
    }

    @Override
    public List<VariableElementImpl> getEnclosedElements() {
        return parameters.get();
    }
}
