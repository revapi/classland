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

import java.util.List;

import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;

import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

public class ErrorTypeImpl extends DeclaredTypeImpl implements ErrorType {
    public ErrorTypeImpl(Universe universe, TypeElementBase source, @Nullable TypeMirrorImpl enclosingType,
            List<TypeMirrorImpl> typeArguments, Memoized<AnnotationSource> annotationSource,
            AnnotationTargetPath path) {
        super(universe, source, enclosingType, typeArguments, annotationSource, path);
    }

    public ErrorTypeImpl(Universe universe, TypeElementBase source, @Nullable TypeMirrorImpl enclosingType,
            List<TypeMirrorImpl> typeArguments, Memoized<List<AnnotationMirrorImpl>> annos) {
        super(universe, source, enclosingType, typeArguments, annos);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.ERROR;
    }
}
