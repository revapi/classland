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

import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;

import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.NoElementImpl;
import org.revapi.classland.impl.model.element.TypeParameterElementImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public class TypeVariableImpl extends TypeMirrorImpl implements TypeVariable {
    private final ElementImpl owner;
    private final MemoizedValue<TypeMirrorImpl> upperBound;
    private final TypeMirrorImpl lowerBound;

    // used to construct a wildcard capture
    public TypeVariableImpl(TypeLookup lookup, @Nullable TypeMirrorImpl upperBound, @Nullable TypeMirrorImpl lowerBound,
            MemoizedValue<List<AnnotationMirrorImpl>> annos) {
        super(lookup, annos);
        this.owner = new NoElementImpl(lookup);
        this.lowerBound = lowerBound == null ? lookup.nullType : lowerBound;
        this.upperBound = obtained(upperBound == null ? TypeMirrorFactory.createJavaLangObject(lookup) : upperBound);
    }

    // used to construct a typevar based on the type parameter
    public TypeVariableImpl(TypeParameterElementImpl owner) {
        super(owner.getLookup(), memoize(owner::getAnnotationMirrors));
        this.owner = owner;
        this.lowerBound = getLookup().nullType;
        // this needs to be lazily evaluated to avoid infinite loop in case of CRTP type params (Cls<T extends Cls<T>>)
        this.upperBound = owner.getLazyBounds().map(bounds -> {
            if (bounds.size() == 1) {
                return bounds.get(0);
            } else {
                return new IntersectionTypeImpl(owner.getLookup(), bounds);
            }
        });
    }

    // used to construct typevar referencing a type parameter in a type-use position
    public TypeVariableImpl(TypeParameterElementImpl owner, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath path, MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(owner.getLookup(), annotationSource, path, typeLookupSeed);
        this.owner = owner;
        this.lowerBound = getLookup().nullType;
        // this needs to be lazily evaluated to avoid infinite loop in case of CRTP type params (Cls<T extends Cls<T>>)
        this.upperBound = owner.getLazyBounds().map(bounds -> {
            if (bounds.size() == 1) {
                return bounds.get(0);
            } else {
                return new IntersectionTypeImpl(owner.getLookup(), bounds);
            }
        });
    }

    private TypeVariableImpl(ElementImpl owner, MemoizedValue<List<AnnotationMirrorImpl>> annos,
            TypeMirrorImpl lowerBound, TypeMirrorImpl upperBound) {
        super(owner.getLookup(), annos);
        this.owner = owner;
        this.upperBound = obtained(upperBound);
        this.lowerBound = lowerBound;
    }

    public TypeVariableImpl rebind(TypeMirrorImpl lowerBound, TypeMirrorImpl upperBound) {
        return new TypeVariableImpl(owner, annos, lowerBound, upperBound);
    }

    @Override
    public @Nullable ElementImpl getSource() {
        return owner;
    }

    public boolean isCaptured() {
        return this.owner.getKind() != ElementKind.TYPE_PARAMETER;
    }

    @Override
    public ElementImpl asElement() {
        return owner;
    }

    @Override
    public TypeMirrorImpl getUpperBound() {
        return upperBound.get();
    }

    public MemoizedValue<TypeMirrorImpl> getUpperBoundValue() {
        return upperBound;
    }

    @Override
    public TypeMirrorImpl getLowerBound() {
        return lowerBound;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.TYPEVAR;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitTypeVariable(this, p);
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
    // TypeVariableImpl that = (TypeVariableImpl) o;
    //
    // if (!owner.equals(that.owner)) {
    // return false;
    // }
    // if (!upperBound.get().equals(that.upperBound.get())) {
    // return false;
    // }
    // return lowerBound.equals(that.lowerBound);
    // }
    //
    // @Override
    // public int hashCode() {
    // int result = super.hashCode();
    // result = 31 * result + owner.hashCode();
    // result = 31 * result + upperBound.get().hashCode();
    // result = 31 * result + lowerBound.hashCode();
    // return result;
    // }
}
