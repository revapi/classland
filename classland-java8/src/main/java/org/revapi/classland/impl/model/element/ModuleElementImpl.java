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

import javax.lang.model.type.TypeKind;

import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.util.Nullable;

public class ModuleElementImpl extends BaseModuleElementImpl {
    public ModuleElementImpl(TypeLookup lookup, Archive archive, @Nullable ClassNode moduleType) {
        super(lookup, archive, moduleType, TypeKind.OTHER);
    }

    public ModuleElementImpl(TypeLookup lookup, Archive archive, String moduleName) {
        this(lookup, archive, moduleName, false);
    }

    protected ModuleElementImpl(TypeLookup lookup, @Nullable Archive archive, String moduleName, boolean unused) {
        super(lookup, archive, moduleName, TypeKind.OTHER);
    }
}
