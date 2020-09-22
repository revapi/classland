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
package org.revapi.classland.impl.model.element;

import java.util.List;

import javax.lang.model.element.TypeElement;

import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

public abstract class TypeElementBase extends ElementImpl implements TypeElement {
    protected final String internalName;
    protected final Memoized<PackageElementImpl> pkg;

    protected TypeElementBase(Universe universe, String internalName, Memoized<PackageElementImpl> pkg) {
        super(universe);
        this.internalName = internalName;
        this.pkg = pkg;
    }

    @Override
    public abstract DeclaredTypeImpl asType();

    @Override
    public abstract TypeMirrorImpl getSuperclass();

    @Override
    public abstract List<TypeMirrorImpl> getInterfaces();

    public abstract @Nullable ExecutableElementImpl getMethod(String methodName, String methodDescriptor);
}
