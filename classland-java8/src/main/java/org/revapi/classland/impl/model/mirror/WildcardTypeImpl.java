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

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;

import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

public class WildcardTypeImpl extends TypeMirrorImpl implements WildcardType {
    private final @Nullable TypeMirrorImpl extendsBound;
    private final @Nullable TypeMirrorImpl superBound;

    public WildcardTypeImpl(Universe universe, @Nullable TypeMirrorImpl extendsBound,
            @Nullable TypeMirrorImpl superBound, Memoized<AnnotationSource> annotationSource,
            AnnotationTargetPath targetPath, Memoized<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(universe, annotationSource, targetPath, typeLookupSeed);
        this.extendsBound = extendsBound;
        this.superBound = superBound;
    }

    @Override
    public TypeMirrorImpl getExtendsBound() {
        return extendsBound;
    }

    @Override
    public TypeMirrorImpl getSuperBound() {
        return superBound;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.WILDCARD;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitWildcard(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        // TODO implement
        return null;
    }
}
