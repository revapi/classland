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

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.AnnotatedConstruct;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.anno.AnnotationFinder;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public abstract class AnnotatedConstructImpl extends BaseModelImpl implements AnnotatedConstruct {
    protected final MemoizedValue<List<AnnotationMirrorImpl>> annos;

    protected AnnotatedConstructImpl(Universe universe, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath path, MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed, boolean isTypeUse) {
        this(universe, annotationSource.map(s -> parseAnnotations(universe, s, path, typeLookupSeed.get(), isTypeUse)));
    }

    protected AnnotatedConstructImpl(Universe universe, MemoizedValue<List<AnnotationMirrorImpl>> annos) {
        super(universe);
        this.annos = annos;
    }

    private static List<AnnotationMirrorImpl> parseAnnotations(Universe universe, AnnotationSource source,
            AnnotationTargetPath path, @Nullable ModuleElementImpl typeLookupSeed, boolean isTypeUse) {
        return AnnotationFinder.find(path, source, isTypeUse).stream()
                .map(a -> new AnnotationMirrorImpl(a, universe, universe
                        .getTypeByInternalNameFromModule(Type.getType(a.desc).getInternalName(), typeLookupSeed)))
                .collect(toList());
    }

    private static List<AnnotationMirrorImpl> parseAnnotations(Universe universe, ClassNode cls,
            ModuleElementImpl typeLookupSeed) {
        return parseAnnotations(universe, AnnotationSource.fromType(cls), AnnotationTargetPath.ROOT, typeLookupSeed,
                false);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return annos.get();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        throw new UnsupportedOperationException();
    }

    protected final List<AnnotationMirrorImpl> parseAnnotations(ClassNode cls, ModuleElementImpl typeLookupSeed) {
        return parseAnnotations(universe, cls, typeLookupSeed);
    }
}
