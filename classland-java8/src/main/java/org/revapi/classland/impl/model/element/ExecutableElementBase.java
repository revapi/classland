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

import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;

import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.AnnotationValueImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public abstract class ExecutableElementBase extends ElementImpl
        implements ExecutableElement, TypeVariableResolutionContext {
    protected ExecutableElementBase(TypeLookup lookup, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath path, MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(lookup, annotationSource, path, typeLookupSeed);
    }

    protected ExecutableElementBase(TypeLookup lookup, MemoizedValue<List<AnnotationMirrorImpl>> annos) {
        super(lookup, annos);
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitExecutable(this, p);
    }

    @Override
    public abstract List<TypeParameterElementImpl> getTypeParameters();

    @Override
    public abstract TypeMirrorImpl getReturnType();

    @Override
    public abstract List<VariableElementImpl> getParameters();

    @Override
    public abstract TypeMirrorImpl getReceiverType();

    @Override
    public abstract List<TypeMirrorImpl> getThrownTypes();

    @Override
    public abstract AnnotationValueImpl getDefaultValue();
}
