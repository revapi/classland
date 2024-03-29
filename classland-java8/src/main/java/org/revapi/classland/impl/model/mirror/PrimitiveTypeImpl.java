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
package org.revapi.classland.impl.model.mirror;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public final class PrimitiveTypeImpl extends TypeMirrorImpl implements PrimitiveType {
    private final TypeKind typeKind;

    public PrimitiveTypeImpl(TypeLookup lookup, TypeKind typeKind) {
        this(lookup, typeKind, AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT,
                MemoizedValue.obtainedNull());
    }

    public PrimitiveTypeImpl(TypeLookup lookup, TypeKind typeKind, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath path, MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(lookup, annotationSource, path, typeLookupSeed);
        this.typeKind = typeKind;
    }

    @Override
    public TypeKind getKind() {
        return typeKind;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitPrimitive(this, p);
    }
    //
    // @Override
    // public boolean equals(Object o) {
    // if (this == o) {
    // return true;
    // }
    // if (!super.equals(o)) {
    // return false;
    // }
    //
    // PrimitiveTypeImpl that = (PrimitiveTypeImpl) o;
    //
    // return typeKind == that.typeKind;
    // }
    //
    // @Override
    // public int hashCode() {
    // int result = super.hashCode();
    // result = 31 * result + typeKind.hashCode();
    // return result;
    // }
}
