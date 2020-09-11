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

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.element.ElementImpl;

public class DeclaredTypeImpl extends TypeMirrorImpl implements DeclaredType {

    public DeclaredTypeImpl(Universe universe) {
        super(universe);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return null;
    }

    @Override
    public ElementImpl asElement() {
        return null;
    }

    @Override
    public TypeMirrorImpl getEnclosingType() {
        return null;
    }

    @Override
    public List<TypeMirrorImpl> getTypeArguments() {
        return null;
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
