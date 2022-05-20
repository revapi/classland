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

import java.util.List;

import javax.lang.model.element.Element;

import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.AnnotatedConstructImpl;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public abstract class ElementImpl extends AnnotatedConstructImpl implements Element {
    protected ElementImpl(TypeLookup lookup, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath path, MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(lookup, annotationSource, path, typeLookupSeed, false);
    }

    protected ElementImpl(TypeLookup lookup, MemoizedValue<List<AnnotationMirrorImpl>> annos) {
        super(lookup, annos);
    }

    public boolean isDeprecated() {
        return false;
    }

    protected boolean isAnnotatedDeprecated() {
        return getAnnotationMirrors().stream().anyMatch(
                a -> a.getAnnotationType().asElement().getQualifiedName().contentEquals("java.lang.Deprecated"));
    }

    @Override
    public abstract TypeMirrorImpl asType();

    @Override
    public abstract NameImpl getSimpleName();

    @Override
    public abstract ElementImpl getEnclosingElement();

    @Override
    public abstract List<? extends ElementImpl> getEnclosedElements();

    // The elements are unique in the universe, so we can use
    // quick identity checks for equality
}
