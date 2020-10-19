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
package org.revapi.classland.impl;

import static java.util.Collections.emptyList;

import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.ArrayTypeImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.NullTypeImpl;
import org.revapi.classland.impl.model.mirror.PrimitiveTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.MemoizedValue;

public class TypesImpl implements Types {
    private final Universe universe;

    public TypesImpl(Universe universe) {
        this.universe = universe;
    }

    @Override
    public Element asElement(TypeMirror t) {
        // TODO implement
        return null;
    }

    @Override
    public boolean isSameType(TypeMirror t1, TypeMirror t2) {
        // TODO implement
        return false;
    }

    @Override
    public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
        // TODO implement
        return false;
    }

    @Override
    public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
        // TODO implement
        return false;
    }

    @Override
    public boolean contains(TypeMirror t1, TypeMirror t2) {
        // TODO implement
        return false;
    }

    @Override
    public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
        return TypeUtils.isSubSignature(m1, m2);
    }

    @Override
    public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
        // TODO implement
        return null;
    }

    @Override
    public TypeMirror erasure(TypeMirror t) {
        return TypeUtils.erasure(t);
    }

    @Override
    public TypeElement boxedClass(PrimitiveType p) {
        // TODO implement
        return null;
    }

    @Override
    public PrimitiveType unboxedType(TypeMirror t) {
        // TODO implement
        return null;
    }

    @Override
    public TypeMirror capture(TypeMirror t) {
        // TODO implement
        return null;
    }

    @Override
    public PrimitiveType getPrimitiveType(TypeKind kind) {
        return TypeMirrorFactory.createPrimitive(universe, kind);
    }

    @Override
    public NullType getNullType() {
        return new NullTypeImpl(universe);
    }

    @Override
    public NoType getNoType(TypeKind kind) {
        return new NoTypeImpl(universe, memoize(Collections::emptyList), kind);
    }

    @Override
    public ArrayType getArrayType(TypeMirror componentType) {
        return new ArrayTypeImpl((TypeMirrorImpl) componentType, -1, AnnotationSource.MEMOIZED_EMPTY,
                AnnotationTargetPath.ROOT, obtainedNull());
    }

    @Override
    public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
        // TODO implement
        return null;
    }

    @Override
    public DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs) {
        // TODO implement
        return null;
    }

    @Override
    public DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem, TypeMirror... typeArgs) {
        // TODO implement
        return null;
    }

    @Override
    public TypeMirror asMemberOf(DeclaredType containing, Element element) {
        // TODO implement
        return null;
    }
}
