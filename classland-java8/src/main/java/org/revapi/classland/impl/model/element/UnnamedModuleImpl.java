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

import java.util.stream.Stream;

import org.revapi.classland.impl.Universe;

public class UnnamedModuleImpl extends ModuleElementImpl {
    public UnnamedModuleImpl(Universe universe) {
        super(universe, (String) null);
    }

    @Override
    public Stream<ReachableModule> getReachableModules() {
        return universe.getModules().stream().filter(m -> m != this).map(m -> new ReachableModule() {
            @Override
            public boolean isTransitive() {
                // we're returning all modules, so no need to be transitive...
                return false;
            }

            @Override
            public String getModuleName() {
                return m.getQualifiedName().toString();
            }
        });
    }
}
