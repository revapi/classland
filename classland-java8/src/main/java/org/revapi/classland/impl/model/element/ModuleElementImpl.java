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

import javax.lang.model.type.TypeKind;

import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.util.Nullable;

public class ModuleElementImpl extends BaseModuleElementImpl {
    public ModuleElementImpl(Universe universe, @Nullable ClassNode moduleType) {
        super(universe, moduleType, TypeKind.OTHER);
    }

    public ModuleElementImpl(Universe universe, String moduleName) {
        super(universe, moduleName, TypeKind.OTHER);
    }
}
