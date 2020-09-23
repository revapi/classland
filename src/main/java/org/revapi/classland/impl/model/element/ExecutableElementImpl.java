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

import static org.revapi.classland.impl.util.Memoized.memoize;
import static org.revapi.classland.impl.util.Memoized.obtained;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import javax.lang.model.element.TypeParameterElement;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.GenericMethodParameters;
import org.revapi.classland.impl.model.signature.SignatureParser;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Modifiers;

public final class ExecutableElementImpl extends ElementImpl
        implements ExecutableElement, TypeVariableResolutionContext {
    private final TypeElementImpl parent;
    private final MethodNode method;
    private final NameImpl name;
    private final Memoized<GenericMethodParameters> signature;
    private final Memoized<TypeMirrorImpl> returnType;
    private final Memoized<List<VariableElementImpl>> parameters;
    private final Memoized<ElementKind> elementKind;
    private final Memoized<Set<Modifier>> modifiers;

    public ExecutableElementImpl(Universe universe, TypeElementImpl parent, MethodNode method) {
        super(universe, obtained(AnnotationSource.fromMethod(method)));
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
                                .map(t -> SignatureParser.parseTypeRef(t.getInternalName())).collect(toList()),
                        method.exceptions.stream().map(SignatureParser::parseTypeRef).collect(toList()), parent);
            } else {
                return SignatureParser.parseMethod(method.signature, parent);
            }
        });

        this.returnType = signature.map(s -> TypeMirrorFactory.create(universe, s.returnType, this));

        this.parameters = parameterTypes.length == 0 ? obtained(emptyList()) : memoize(() -> {
            List<VariableElementImpl> ret = new ArrayList<>(parameterTypes.length);
            for (int i = 0; i < parameterTypes.length; ++i) {
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
    }

    MethodNode getNode() {
        return method;
    }

    Memoized<GenericMethodParameters> getSignature() {
        return signature;
    }

    @Override
    public Optional<TypeParameterElementImpl> resolveTypeVariable(String name) {
        // TODO implement
        return Optional.empty();
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        // TODO implement
        return null;
    }

    @Override
    public TypeMirrorImpl getReturnType() {
        // TODO implement
        return null;
    }

    @Override
    public List<VariableElementImpl> getParameters() {
        return parameters.get();
    }

    @Override
    public TypeMirrorImpl getReceiverType() {
        // TODO implement
        return null;
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
        // TODO implement
        return null;
    }

    @Override
    public AnnotationValue getDefaultValue() {
        // TODO implement
        return null;
    }

    @Override
    public TypeMirrorImpl asType() {
        // TODO implement
        return null;
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
        return name;
    }

    @Override
    public Element getEnclosingElement() {
        return parent;
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return parameters.get();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitExecutable(this, p);
    }
}
