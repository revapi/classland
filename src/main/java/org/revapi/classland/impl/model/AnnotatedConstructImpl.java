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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import static org.revapi.classland.impl.util.Memoized.memoize;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.AnnotatedConstruct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.impl.model.anno.AnnotationFinder;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.util.Nullable;

public abstract class AnnotatedConstructImpl extends BaseModelImpl implements AnnotatedConstruct {
    @Deprecated
    protected AnnotatedConstructImpl(Universe universe) {
        super(universe);
    }

    protected AnnotatedConstructImpl(Universe universe, AnnotationTargetPath path) {
        super(universe);
    }

    @Deprecated
    public static List<AnnotationMirrorImpl> parseAnnotations(Universe universe,
            @Nullable List<? extends AnnotationNode> annos) {
        return annos == null ? Collections.emptyList()
                : annos.stream().map(a -> new AnnotationMirrorImpl(a, universe)).collect(toList());
    }

    public static List<AnnotationMirrorImpl> parseAnnotations(Universe universe, AnnotationSource source,
            AnnotationTargetPath path) {
        return AnnotationFinder.find(path, source).stream().map(a -> new AnnotationMirrorImpl(a, universe))
                .collect(toList());
    }

    @Deprecated
    @SafeVarargs
    public static List<AnnotationMirrorImpl> parseMoreAnnotations(Universe universe,
            @Nullable List<? extends AnnotationNode>... annos) {
        if (annos == null || annos.length == 0) {
            return Collections.emptyList();
        }

        List<AnnotationMirrorImpl> ret = new ArrayList<>();
        for (List<? extends AnnotationNode> l : annos) {
            if (l == null)
                continue;

            ret.addAll(parseAnnotations(universe, l));
        }

        return ret;
    }

    public static List<AnnotationMirrorImpl> parseAnnotations(Universe universe, ClassNode cls) {
        return parseAnnotations(universe, AnnotationSource.fromType(cls), new AnnotationTargetPath(null));
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

    @Deprecated
    @SafeVarargs
    protected final List<AnnotationMirrorImpl> parseMoreAnnotations(@Nullable List<? extends AnnotationNode>... annos) {
        return parseMoreAnnotations(universe, annos);
    }

    protected final List<AnnotationMirrorImpl> parseAnnotations(ClassNode cls) {
        return parseAnnotations(universe, cls);
    }
}
