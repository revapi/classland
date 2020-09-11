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

import static org.revapi.classland.impl.util.Memoized.memoize;

import java.util.List;
import java.util.function.Supplier;

import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.util.Memoized;

public class NoTypeImpl extends TypeMirrorImpl implements NoType {
    private final TypeKind kind;
    private final Memoized<List<AnnotationMirrorImpl>> annos;

    public NoTypeImpl(Universe universe, Supplier<List<AnnotationMirrorImpl>> annos, TypeKind kind) {
        super(universe);
        this.kind = kind;
        this.annos = memoize(annos);
    }

    @Override
    public TypeKind getKind() {
        return kind;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitNoType(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return annos.get();
    }
}
