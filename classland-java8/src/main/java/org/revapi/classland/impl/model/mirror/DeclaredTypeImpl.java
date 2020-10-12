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

import static java.util.Collections.emptyList;

import static org.revapi.classland.impl.util.MemoizedValue.obtained;

import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public class DeclaredTypeImpl extends TypeMirrorImpl implements DeclaredType {
    private final TypeElementBase source;
    private final TypeMirrorImpl enclosingType;
    private final List<TypeMirrorImpl> typeArguments;

    public DeclaredTypeImpl(Universe universe, TypeElementBase source, @Nullable TypeMirrorImpl enclosingType,
            List<TypeMirrorImpl> typeArguments, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath path) {
        super(universe, annotationSource, path, source.lookupModule());
        this.source = source;
        this.enclosingType = enclosingType == null ? new NoTypeImpl(universe, obtained(emptyList()), TypeKind.NONE)
                : enclosingType;
        this.typeArguments = typeArguments;
    }

    public DeclaredTypeImpl(Universe universe, TypeElementBase source, @Nullable TypeMirrorImpl enclosingType,
            List<TypeMirrorImpl> typeArguments, MemoizedValue<List<AnnotationMirrorImpl>> annos) {
        super(universe, annos);
        this.source = source;
        this.enclosingType = enclosingType == null ? new NoTypeImpl(universe, obtained(emptyList()), TypeKind.NONE)
                : enclosingType;
        this.typeArguments = typeArguments;
    }

    @Override
    public TypeElementBase asElement() {
        return source;
    }

    @Override
    public TypeMirrorImpl getEnclosingType() {
        return enclosingType;
    }

    @Override
    public List<TypeMirrorImpl> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.DECLARED;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitDeclared(this, p);
    }
}
