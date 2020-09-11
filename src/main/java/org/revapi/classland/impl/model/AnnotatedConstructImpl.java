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
package org.revapi.classland.impl.model;

import static org.revapi.classland.impl.util.Memoized.memoize;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.AnnotatedConstruct;

import org.objectweb.asm.tree.AnnotationNode;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.util.Nullable;

public abstract class AnnotatedConstructImpl extends BaseModelImpl implements AnnotatedConstruct {
    protected AnnotatedConstructImpl(Universe universe) {
        super(universe);
    }

    @Override
    public abstract List<AnnotationMirrorImpl> getAnnotationMirrors();

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        throw new UnsupportedOperationException();
    }

    protected List<AnnotationMirrorImpl> parseAnnotations(@Nullable List<AnnotationNode> annos) {
        return annos == null ? Collections.emptyList()
                : annos.stream()
                        .map(a -> new AnnotationMirrorImpl(a, universe,
                                memoize(() -> universe.getDeclaredTypeByInternalName(a.desc))))
                        .collect(Collectors.toList());
    }
}
