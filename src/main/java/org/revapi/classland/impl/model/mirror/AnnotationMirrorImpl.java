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

import org.objectweb.asm.tree.AnnotationNode;
import org.revapi.classland.impl.model.BaseModelImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.element.ExecutableElementImpl;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public final class AnnotationMirrorImpl extends BaseModelImpl implements AnnotationMirror {
    private final AnnotationNode node;
    private final Supplier<DeclaredTypeImpl> getAnnotationType;

    public AnnotationMirrorImpl(AnnotationNode node, Universe universe, Supplier<DeclaredTypeImpl> getAnnotationType) {
        super(universe);
        this.node = node;
        this.getAnnotationType = getAnnotationType;
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
