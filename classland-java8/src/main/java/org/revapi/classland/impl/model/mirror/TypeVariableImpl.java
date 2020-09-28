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

import java.util.Collections;
import java.util.List;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.NoElementImpl;
import org.revapi.classland.impl.model.element.TypeParameterElementImpl;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

public class TypeVariableImpl extends TypeMirrorImpl implements TypeVariable {
    private final ElementImpl owner;
    private final TypeMirrorImpl upperBound;
    private final TypeMirrorImpl lowerBound;

    // used to construct a wildcard capture
    public TypeVariableImpl(Universe universe, @Nullable TypeMirrorImpl upperBound, @Nullable TypeMirrorImpl lowerBound,
            Memoized<AnnotationSource> annotationSource, AnnotationTargetPath path) {
        // TODO the unnamed module is wrong here, but this ctor is not used yet as of now, so I've put it here so that
        // stuff compiles. Revisit once this ctor is actually used.
        super(universe, annotationSource, path, memoize(universe::getUnnamedModule));
        this.owner = new NoElementImpl(universe);
        this.upperBound = upperBound == null ? new NullTypeImpl(universe) : upperBound;
        // TODO the unnamed module is most probably wrong as a lookup seed here, too
        this.lowerBound = lowerBound == null
                ? TypeMirrorFactory.create(universe, Universe.JAVA_LANG_OBJECT_SIG, TypeVariableResolutionContext.EMPTY,
                        AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, memoize(universe::getUnnamedModule))
                : lowerBound;
    }

    // used to construct a typevar based on the type parameter
    public TypeVariableImpl(TypeParameterElementImpl owner) {
        super(owner.getUniverse(), memoize(owner::getAnnotationMirrors));
        this.owner = owner;
        this.upperBound = new NullTypeImpl(universe);
        List<TypeMirrorImpl> bounds = owner.getBounds();
        if (bounds.size() == 1) {
            this.lowerBound = bounds.get(0);
        } else {
            this.lowerBound = new IntersectionTypeImpl(owner.getUniverse(), bounds);
        }
    }

    @Override
    public ElementImpl asElement() {
        return owner;
    }

    @Override
    public TypeMirrorImpl getUpperBound() {
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

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        // TODO implement
        return Collections.emptyList();
    }
}
