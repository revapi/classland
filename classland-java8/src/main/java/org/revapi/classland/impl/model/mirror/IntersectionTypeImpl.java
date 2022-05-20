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

import static java.util.Collections.emptyList;

import static org.revapi.classland.impl.util.MemoizedValue.obtained;

import java.util.List;

import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.TypeLookup;

public class IntersectionTypeImpl extends TypeMirrorImpl implements IntersectionType {
    private final List<TypeMirrorImpl> bounds;

    public IntersectionTypeImpl(TypeLookup lookup, List<TypeMirrorImpl> bounds) {
        super(lookup, obtained(emptyList()));
        this.bounds = bounds;
    }

    @Override
    public List<TypeMirrorImpl> getBounds() {
        return bounds;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.INTERSECTION;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitIntersection(this, p);
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
    // IntersectionTypeImpl that = (IntersectionTypeImpl) o;
    //
    // return bounds.equals(that.bounds);
    // }
    //
    // @Override
    // public int hashCode() {
    // int result = super.hashCode();
    // result = 31 * result + bounds.hashCode();
    // return result;
    // }
}
