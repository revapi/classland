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
package org.revapi.classland.impl.model.element;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

import static org.objectweb.asm.TypeReference.CLASS_TYPE_PARAMETER;
import static org.objectweb.asm.TypeReference.newTypeParameterReference;
import static org.revapi.classland.impl.util.MemoizedValue.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;

import org.objectweb.asm.TypeReference;
import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.mirror.TypeVariableImpl;
import org.revapi.classland.impl.model.signature.TypeParameterBound;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.MemoizedValue;

public final class TypeParameterElementImpl extends ElementImpl implements TypeParameterElement {
    private final NameImpl name;
    private final TypeVariableResolutionContext owner;
    private final MemoizedValue<List<TypeMirrorImpl>> bounds;
    private final TypeParameterBound rawBound;

    protected TypeParameterElementImpl(TypeLookup lookup, String name, TypeVariableResolutionContext owner,
            TypeParameterBound bound, int index) {
        super(lookup, owner.asAnnotationSource(),
                new AnnotationTargetPath(newTypeParameterReference(CLASS_TYPE_PARAMETER, index)), owner.lookupModule());

        this.name = NameImpl.of(name);
        this.owner = owner;
        this.rawBound = bound;
        this.bounds = memoize(() -> {
            switch (bound.boundType) {
            case UNBOUNDED:
                return singletonList(TypeMirrorFactory.create(lookup, TypeLookup.JAVA_LANG_OBJECT_SIG, owner,
                        AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, owner.lookupModule()));
            case EXACT:
                return singletonList(
                        TypeMirrorFactory.create(lookup, bound.classBound, owner, owner.asAnnotationSource(),
                                new AnnotationTargetPath(newTypeParameterReference(CLASS_TYPE_PARAMETER, index)),
                                owner.lookupModule()));
            case EXTENDS:
                List<TypeMirrorImpl> ret = new ArrayList<>(bound.interfaceBounds.size() + 1);
                if (bound.classBound == null) {
                    ret.add(TypeMirrorFactory.createJavaLangObject(lookup));
                } else {
                    ret.add(TypeMirrorFactory.create(lookup, bound.classBound, owner, owner.asAnnotationSource(),
                            new AnnotationTargetPath(
                                    TypeReference.newTypeParameterBoundReference(CLASS_TYPE_PARAMETER, index, 0)),
                            owner.lookupModule()));
                }
                int i = 1;
                for (TypeSignature b : bound.interfaceBounds) {
                    ret.add(TypeMirrorFactory.create(lookup, b, owner, owner.asAnnotationSource(),
                            new AnnotationTargetPath(
                                    TypeReference.newTypeParameterBoundReference(CLASS_TYPE_PARAMETER, index, i++)),
                            owner.lookupModule()));
                }
                return ret;
            case SUPER:
                throw new IllegalStateException(
                        "Cannot understand a type parameter with super bounds." + " Has java changed that much?");
            default:
                throw new IllegalStateException("Unhandled bound type: " + bound.boundType);
            }
        });
    }

    public TypeParameterBound getRawBound() {
        return rawBound;
    }

    @Override
    public TypeMirrorImpl asType() {
        return new TypeVariableImpl(this);
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.TYPE_PARAMETER;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return emptySet();
    }

    @Override
    public NameImpl getSimpleName() {
        return name;
    }

    @Override
    public ElementImpl getGenericElement() {
        return owner.asElement();
    }

    @Override
    public List<TypeMirrorImpl> getBounds() {
        return bounds.get();
    }

    public MemoizedValue<List<TypeMirrorImpl>> getLazyBounds() {
        return bounds;
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return getGenericElement();
    }

    @Override
    public List<ElementImpl> getEnclosedElements() {
        return emptyList();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitTypeParameter(this, p);
    }
}
