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
        // TODO implement
        return false;
    }

    @Override
    public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
        // TODO implement
        return null;
    }

    @Override
    public TypeMirror erasure(TypeMirror t) {
        // TODO implement
        return null;
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
        // TODO implement
        return null;
    }

    @Override
    public NullType getNullType() {
        // TODO implement
        return null;
    }

    @Override
    public NoType getNoType(TypeKind kind) {
        // TODO implement
        return null;
    }

    @Override
    public ArrayType getArrayType(TypeMirror componentType) {
        // TODO implement
        return null;
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
