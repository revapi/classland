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
package org.revapi.classland.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.revapi.classland.impl.model.element.ModuleElementImpl;

public class ReachableModulesIterator implements Iterator<ModuleElementImpl> {
    private final TypeLookup lookup;
    private final Set<String> visited = new HashSet<>();
    private final List<ModuleElementImpl.ReachableModule> nexts = new ArrayList<>();
    private ModuleElementImpl nextModule;

    public ReachableModulesIterator(TypeLookup lookup, ModuleElementImpl startingModule) {
        this.lookup = lookup;
        startingModule.getReachableModules().forEach(nexts::add);
        findNextModule();
    }

    @Override
    public boolean hasNext() {
        return nextModule != null;
    }

    @Override
    public ModuleElementImpl next() {
        if (nextModule == null) {
            throw new NoSuchElementException();
        }
        ModuleElementImpl ret = nextModule;
        findNextModule();
        return ret;
    }

    private void findNextModule() {
        if (nexts.isEmpty()) {
            nextModule = null;
            return;
        }
        ModuleElementImpl.ReachableModule reachable = nexts.remove(0);
        String next = reachable.getModuleName();
        if (next == null) {
            findNextModule();
            return;
        }

        ModuleElementImpl m = lookup.getModule(next);

        if (reachable.isTransitive()) {
            if (m != null) {
                m.getReachableModules().forEach(r -> {
                    if (!visited.contains(r.getModuleName())) {
                        nexts.add(r);
                    }
                });
            }
        }

        nextModule = m;
        visited.add(next);
    }
}
