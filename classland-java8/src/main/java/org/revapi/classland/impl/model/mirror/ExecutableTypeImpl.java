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
package org.revapi.classland.impl.model.mirror;

import static java.util.stream.Collectors.toList;

import static org.revapi.classland.impl.util.MemoizedValue.memoize;

import java.util.List;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.ExecutableElementBase;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public class ExecutableTypeImpl extends TypeMirrorImpl implements ExecutableType {
    private final List<TypeVariableImpl> typeVars;
    private final TypeMirrorImpl returnType;
    private final List<TypeMirrorImpl> parameterTypes;
    private final TypeMirrorImpl receiverType;
    private final List<TypeMirrorImpl> thrownTypes;

    public ExecutableTypeImpl(ExecutableElementBase source) {
        super(source.getUniverse(), memoize(source::getAnnotationMirrors));
        typeVars = source.getTypeParameters().stream().map(TypeVariableImpl::new).collect(toList());
        returnType = source.getReturnType();
        parameterTypes = source.getParameters().stream().map(ElementImpl::asType).collect(toList());
        receiverType = source.getReceiverType();
        thrownTypes = source.getThrownTypes();
    }

    public ExecutableTypeImpl(Universe universe, List<TypeVariableImpl> typeVariables, TypeMirrorImpl returnType,
            TypeMirrorImpl receiverType, List<TypeMirrorImpl> parameterTypes, List<TypeMirrorImpl> thrownTypes,
            MemoizedValue<AnnotationSource> annotationSource, AnnotationTargetPath path,
            MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(universe, annotationSource, path, typeLookupSeed);

        this.typeVars = typeVariables;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.receiverType = receiverType;
        this.thrownTypes = thrownTypes;
    }

    @Override
    public List<TypeVariableImpl> getTypeVariables() {
        return typeVars;
    }

    @Override
    public TypeMirrorImpl getReturnType() {
        return returnType;
    }

    @Override
    public List<TypeMirrorImpl> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public TypeMirrorImpl getReceiverType() {
        return receiverType;
    }

    @Override
    public List<TypeMirrorImpl> getThrownTypes() {
        return thrownTypes;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.EXECUTABLE;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitExecutable(this, p);
    }
}
