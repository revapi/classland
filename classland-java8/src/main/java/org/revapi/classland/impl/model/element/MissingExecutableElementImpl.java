/*
 * Copyright 2020-2021 Lukas Krejci
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationValueImpl;
import org.revapi.classland.impl.model.mirror.ExecutableTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.SignatureParser;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public class MissingExecutableElementImpl extends ExecutableElementBase {
    private final TypeElementBase parent;
    private final NameImpl name;
    private final TypeMirrorImpl returnType;
    private final List<VariableElementImpl> paramTypes;
    private final ExecutableTypeImpl type;

    public MissingExecutableElementImpl(Universe universe, TypeElementBase parent, String name,
            String returnTypeDescriptor, List<String> parameterDescriptors) {
        super(universe, AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, parent.lookupModule());
        this.parent = parent;
        this.name = NameImpl.of(name);
        this.returnType = TypeMirrorFactory.create(universe, SignatureParser.parseTypeRef(returnTypeDescriptor), this,
                AnnotationTargetPath.ROOT);
        List<VariableElementImpl> params = new ArrayList<>(parameterDescriptors.size());
        for (int i = 0; i < parameterDescriptors.size(); ++i) {
            params.add(new VariableElementImpl.Missing(universe, parent.lookupModule(), this, "arg" + i,
                    parameterDescriptors.get(i), ElementKind.PARAMETER));
        }
        this.paramTypes = Collections.unmodifiableList(params);
        this.type = new ExecutableTypeImpl(this);
    }

    @Override
    public List<TypeParameterElementImpl> getTypeParameters() {
        return emptyList();
    }

    @Override
    public TypeMirrorImpl getReturnType() {
        return returnType;
    }

    @Override
    public List<VariableElementImpl> getParameters() {
        return paramTypes;
    }

    @Override
    public TypeMirrorImpl getReceiverType() {
        return parent.asType();
    }

    @Override
    public boolean isVarArgs() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public List<TypeMirrorImpl> getThrownTypes() {
        return emptyList();
    }

    @Override
    public AnnotationValueImpl getDefaultValue() {
        return null;
    }

    @Override
    public TypeMirrorImpl asType() {
        return type;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.METHOD;
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
    public ElementImpl getEnclosingElement() {
        return parent;
    }

    @Override
    public List<? extends ElementImpl> getEnclosedElements() {
        return paramTypes;
    }

    @Override
    public Optional<TypeParameterElementImpl> resolveTypeVariable(String name) {
        return Optional.empty();
    }

    @Override
    public MemoizedValue<AnnotationSource> asAnnotationSource() {
        return AnnotationSource.MEMOIZED_EMPTY;
    }

    @Override
    public MemoizedValue<@Nullable ModuleElementImpl> lookupModule() {
        return parent.lookupModule();
    }

    @Override
    public ElementImpl asElement() {
        return this;
    }
}
