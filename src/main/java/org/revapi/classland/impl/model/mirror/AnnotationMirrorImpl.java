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
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.impl.model.BaseModelImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.element.ExecutableElementImpl;

public final class AnnotationMirrorImpl extends BaseModelImpl implements AnnotationMirror {
    private final AnnotationNode node;
    private final Supplier<DeclaredTypeImpl> getAnnotationType;

    public AnnotationMirrorImpl(AnnotationNode node, Universe universe, Supplier<DeclaredTypeImpl> getAnnotationType) {
        super(universe);
        this.node = node;
        this.getAnnotationType = getAnnotationType;
    }

    public static List<AnnotationMirrorImpl> convert(Universe universe, List<AnnotationNode> annos) {
        return annos == null ? Collections.emptyList()
                : annos.stream()
                        .map(a -> new AnnotationMirrorImpl(a, universe,
                                memoize(() -> universe.getDeclaredTypeByInternalName(a.desc))))
                        .collect(Collectors.toList());
    }

    public static List<AnnotationMirrorImpl> parse(Universe universe, ClassNode cls) {
        return convert(universe, cls.visibleAnnotations);
    }

    @Override
    public DeclaredTypeImpl getAnnotationType() {
        return getAnnotationType.get();
    }

    @Override
    public Map<ExecutableElementImpl, ? extends AnnotationValue> getElementValues() {
        // TODO implement
        return Collections.emptyMap();
    }
}
