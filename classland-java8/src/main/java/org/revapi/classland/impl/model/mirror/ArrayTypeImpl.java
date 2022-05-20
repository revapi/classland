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

import java.util.List;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public class ArrayTypeImpl extends TypeMirrorImpl implements ArrayType {

    private final TypeMirrorImpl componentType;

    public ArrayTypeImpl(TypeMirrorImpl componentType, int currentDimension,
            MemoizedValue<AnnotationSource> annotationSource, AnnotationTargetPath typepath,
            MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(componentType.getLookup(), annotationSource, arrayize(typepath, currentDimension), typeLookupSeed);
        this.componentType = componentType;
    }

    private ArrayTypeImpl(TypeMirrorImpl componentType, MemoizedValue<List<AnnotationMirrorImpl>> annos) {
        super(componentType.getLookup(), annos);
        this.componentType = componentType;
    }

    private static AnnotationTargetPath arrayize(AnnotationTargetPath path, int dimensions) {
        AnnotationTargetPath ret = path.clone();
        while (--dimensions > 0) {
            ret = ret.array();
        }

        return ret;
    }

    /**
     * Returns a copy of this array with the provided type as its component.
     *
     * Be careful with this if this is a multidimensional array. The supplied component type must then be of the same
     * array dimension as the original component type of this array, otherwise annotation mapping will break.
     *
     * @param componentType
     *            the new component type
     * 
     * @return the copied array type
     */
    public ArrayTypeImpl withComponentType(TypeMirrorImpl componentType) {
        return new ArrayTypeImpl(componentType, annos);
    }

    @Override
    public TypeMirrorImpl getComponentType() {
        return componentType;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.ARRAY;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitArray(this, p);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }

        ArrayTypeImpl arrayType = (ArrayTypeImpl) o;

        return componentType.equals(arrayType.componentType);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + componentType.hashCode();
        return result;
    }
}
