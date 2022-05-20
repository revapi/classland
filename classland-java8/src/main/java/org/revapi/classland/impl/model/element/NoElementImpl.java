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

import static org.revapi.classland.impl.util.MemoizedValue.obtained;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;

public class NoElementImpl extends ElementImpl {
    public NoElementImpl(TypeLookup lookup) {
        super(lookup, AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, obtained(lookup.getUnnamedModule()));
    }

    @Override
    public TypeMirrorImpl asType() {
        return new NoTypeImpl(lookup, obtained(emptyList()), TypeKind.NONE);
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.OTHER;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public NameImpl getSimpleName() {
        return NameImpl.EMPTY;
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return null;
    }

    @Override
    public List<? extends ElementImpl> getEnclosedElements() {
        return emptyList();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitUnknown(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return emptyList();
    }
}
