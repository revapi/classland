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

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;

import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public class WildcardTypeImpl extends TypeMirrorImpl implements WildcardType {
    private final @Nullable TypeMirrorImpl extendsBound;
    private final @Nullable TypeMirrorImpl superBound;

    public WildcardTypeImpl(TypeLookup lookup, @Nullable TypeMirrorImpl extendsBound,
            @Nullable TypeMirrorImpl superBound, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath targetPath, MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(lookup, annotationSource, targetPath, typeLookupSeed);
        this.extendsBound = extendsBound;
        this.superBound = superBound;
    }

    private WildcardTypeImpl(TypeLookup lookup, @Nullable TypeMirrorImpl extendsBound,
            @Nullable TypeMirrorImpl superBound, MemoizedValue<List<AnnotationMirrorImpl>> annos) {
        super(lookup, annos);
        this.extendsBound = extendsBound;
        this.superBound = superBound;
    }

    public WildcardTypeImpl rebind(@Nullable TypeMirrorImpl extendsBound, @Nullable TypeMirrorImpl superBound) {
        return new WildcardTypeImpl(lookup, extendsBound, superBound, annos);
    }

    public TypeVariableImpl capture() {
        return new TypeVariableImpl(lookup, getExtendsBound(), null, annos);
    }

    public boolean isExtends() {
        // we consider an unbound wildcard an extends of object
        return extendsBound != null || superBound == null;
    }

    public boolean isSuper() {
        // we consider an unbound wildcard a super of object
        return superBound != null || extendsBound == null;
    }

    public boolean isUnbound() {
        return extendsBound == null && superBound == null;
    }

    @Override
    @Nullable
    public TypeMirrorImpl getExtendsBound() {
        return extendsBound;
    }

    @Override
    @Nullable
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
    //
    // @Override
    // public boolean equals(Object o) {
    // if (this == o) {
    // return true;
    // }
    // if (!(o instanceof WildcardTypeImpl)) {
    // return false;
    // }
    // if (!super.equals(o)) {
    // return false;
    // }
    //
    // WildcardTypeImpl that = (WildcardTypeImpl) o;
    //
    // if (!Objects.equals(extendsBound, that.extendsBound)) {
    // return false;
    // }
    //
    // return Objects.equals(superBound, that.superBound);
    // }
    //
    // @Override
    // public int hashCode() {
    // int result = super.hashCode();
    // result = 31 * result + (extendsBound != null ? extendsBound.hashCode() : 0);
    // result = 31 * result + (superBound != null ? superBound.hashCode() : 0);
    // return result;
    // }
}
