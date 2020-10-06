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
package org.revapi.classland.impl.model.signature;

import java.util.Optional;

import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.TypeParameterElementImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public interface TypeVariableResolutionContext {

    Optional<TypeParameterElementImpl> resolveTypeVariable(String name);

    MemoizedValue<AnnotationSource> asAnnotationSource();

    MemoizedValue<@Nullable ModuleElementImpl> lookupModule();

    ElementImpl asElement();
}
