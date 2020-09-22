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

import java.util.Collections;
import java.util.List;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.util.Nullable;

public class DeclaredTypeImpl extends TypeMirrorImpl implements DeclaredType {
    private final ElementImpl source;
    private final TypeMirrorImpl enclosingType;
    private final List<TypeMirrorImpl> typeArguments;

    public DeclaredTypeImpl(Universe universe, ElementImpl source, @Nullable TypeMirrorImpl enclosingType,
            List<TypeMirrorImpl> typeArguments) {
        super(universe);
        this.source = source;
        this.enclosingType = enclosingType == null
                ? new NoTypeImpl(universe, Collections::emptyList, TypeKind.NONE)
                : enclosingType;
        this.typeArguments = typeArguments;
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        // TODO implement
        return Collections.emptyList();
    }

    @Override
    public ElementImpl asElement() {
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
