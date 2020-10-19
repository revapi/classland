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
package org.revapi.classland.impl.util;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor8;

public class TypePairVisitor<R> extends SimpleTypeVisitor8<R, TypeMirror> {
    protected TypePairVisitor() {
    }

    protected TypePairVisitor(R defaultValue) {
        super(defaultValue);
    }

    protected R unmatchedAction(TypeMirror a, TypeMirror b) {
        return super.defaultAction(a, b);
    }

    @Override
    protected R defaultAction(TypeMirror e, TypeMirror b) {
        return super.defaultAction(e, b);
    }

    @Override
    public final R visitIntersection(IntersectionType t, TypeMirror b) {
        return b instanceof IntersectionType ? visitIntersection(t, (IntersectionType) b) : unmatchedAction(t, b);
    }

    public R visitIntersection(IntersectionType a, IntersectionType b) {
        return defaultAction(a, b);
    }

    @Override
    public final R visitUnion(UnionType t, TypeMirror b) {
        return b instanceof UnionType ? visitUnion(t, (UnionType) b) : unmatchedAction(t, b);
    }

    public R visitUnion(UnionType t, UnionType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitPrimitive(PrimitiveType t, TypeMirror b) {
        return b instanceof PrimitiveType ? visitPrimitive(t, (PrimitiveType) b) : unmatchedAction(t, b);
    }

    public R visitPrimitive(PrimitiveType t, PrimitiveType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitNull(NullType t, TypeMirror b) {
        return b instanceof NullType ? visitNull(t, (NullType) b) : unmatchedAction(t, b);
    }

    public R visitNull(NullType t, NullType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitArray(ArrayType t, TypeMirror b) {
        return b instanceof ArrayType ? visitArray(t, (ArrayType) b) : unmatchedAction(t, b);
    }

    public R visitArray(ArrayType t, ArrayType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitDeclared(DeclaredType t, TypeMirror b) {
        return b instanceof DeclaredType ? visitDeclared(t, (DeclaredType) b) : unmatchedAction(t, b);
    }

    public R visitDeclared(DeclaredType t, DeclaredType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitError(ErrorType t, TypeMirror b) {
        return b instanceof ErrorType ? visitError(t, (ErrorType) b) : unmatchedAction(t, b);
    }

    public R visitError(ErrorType t, ErrorType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitTypeVariable(TypeVariable t, TypeMirror b) {
        return b instanceof TypeVariable ? visitTypeVariable(t, (TypeVariable) b) : unmatchedAction(t, b);
    }

    public R visitTypeVariable(TypeVariable t, TypeVariable b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitWildcard(WildcardType t, TypeMirror b) {
        return b instanceof WildcardType ? visitWildcard(t, (WildcardType) b) : unmatchedAction(t, b);
    }

    public R visitWildcard(WildcardType t, WildcardType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitExecutable(ExecutableType t, TypeMirror b) {
        return b instanceof ExecutableType ? visitExecutable(t, (ExecutableType) b) : unmatchedAction(t, b);
    }

    public R visitExecutable(ExecutableType t, ExecutableType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitNoType(NoType t, TypeMirror b) {
        return b instanceof NoType ? visitNoType(t, (NoType) b) : unmatchedAction(t, b);
    }

    public R visitNoType(NoType t, NoType b) {
        return defaultAction(t, b);
    }

    @Override
    public final R visitUnknown(TypeMirror t, TypeMirror b) {
        if (b != null && t.getClass() == b.getClass()) {
            return defaultAction(t, b);
        } else {
            return unmatchedAction(t, b);
        }
    }
}
