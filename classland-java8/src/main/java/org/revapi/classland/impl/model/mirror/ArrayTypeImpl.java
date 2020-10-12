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

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
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
        super(componentType.getUniverse(), annotationSource, arrayize(typepath, currentDimension), typeLookupSeed);
        this.componentType = componentType;
    }

    private static AnnotationTargetPath arrayize(AnnotationTargetPath path, int dimensions) {
        AnnotationTargetPath ret = path.clone();
        while (--dimensions > 0) {
            ret = ret.array();
        }

        return ret;
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
}
