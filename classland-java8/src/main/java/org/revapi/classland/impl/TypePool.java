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

import static org.revapi.classland.impl.util.ByteCode.parseClass;
import static org.revapi.classland.impl.util.Exceptions.failWithRuntimeException;
import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;
import org.revapi.classland.archive.ModuleResolver;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.element.UnnamedModuleImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public final class TypePool implements AutoCloseable {

    private final UnnamedModuleImpl unnamedModule;
    private final boolean analyzeModules;
    private final Set<Archive> archives = new HashSet<>();
    private final Set<ModuleResolver> moduleResolvers = new HashSet<>();
    private final Map<String, ModuleElementImpl> modules = new HashMap<>();
    private final TypeLookup lookup;

    public TypePool(boolean analyzeModules) {
        this.analyzeModules = analyzeModules;
        lookup = new TypeLookup(this);
        unnamedModule = new UnnamedModuleImpl(getLookup());
        modules.put("", unnamedModule);
    }

    public TypeLookup getLookup() {
        return lookup;
    }

    public @Nullable ModuleElementImpl getModule(String name) {
        return modules.get(name);
    }

    public UnnamedModuleImpl getUnnamedModule() {
        return unnamedModule;
    }

    public Collection<ModuleElementImpl> getModules() {
        return new ArrayList<>(modules.values());
    }

    ModuleElementImpl getJavaBase() {
        ModuleElementImpl javaBase = analyzeModules ? modules.get("java.base") : unnamedModule;
        if (javaBase == null) {
            javaBase = unnamedModule;
        }

        return javaBase;
    }

    public void registerArchive(Archive archive) {
        synchronized (modules) {
            archives.add(archive);
            ArchiveContents contents = new ArchiveContents(archive);
            ModuleElementImpl module = parseNewModule(contents);
            if (module != unnamedModule) {
                modules.putIfAbsent(module.getQualifiedName().asString(), module);
            }

            module.addPackageGatherer(pkgs -> contents.getPackages().forEach((name, data) -> {
                PackageElementImpl pkg = pkgs.computeIfAbsent(name,
                        __ -> new PackageElementImpl(lookup, name, lazyParse(data), analyzeModules ? module : null));
                pkg.addTypeGatherer(() -> contents.getTypes().get(name).stream()
                        .map(cd -> new TypeElementImpl(lookup, archive, cd.getName(), lazyParse(cd), pkg))
                        .collect(Collectors.toList()));
            }));
        }
    }

    private ModuleElementImpl parseNewModule(ArchiveContents contents) {
        return contents.getModule().map(cd -> {
            if (!analyzeModules) {
                return unnamedModule;
            }
            ClassNode cls = eagerParse(cd);
            return new ModuleElementImpl(lookup, contents.getArchive(), cls);
        }).orElseGet(() -> contents.getAutomaticModuleName()
                .map(n -> analyzeModules ? new ModuleElementImpl(lookup, contents.getArchive(), n) : unnamedModule)
                .orElse(unnamedModule));
    }

    public void addModule(String name) {
        for (ModuleResolver r : moduleResolvers) {
            try {
                r.getModuleArchive(name).ifPresent(this::registerArchive);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public void registerModuleResolver(ModuleResolver resolver) {
        moduleResolvers.add(resolver);
    }

    public void addModulesClosure() {
        Set<String> todo = new HashSet<>(modules.keySet());

        while (!todo.isEmpty()) {
            Iterator<String> it = todo.iterator();
            while (it.hasNext()) {
                String moduleName = it.next();
                it.remove();
                ModuleElementImpl m = modules.get(moduleName);
                if (m == null) {
                    continue;
                }

                m.getReachableModules().forEach(reachable -> {
                    String dep = reachable.getModuleName();
                    addModule(dep);

                    if (reachable.isTransitive()) {
                        todo.add(reachable.getModuleName());
                    }
                });
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (Archive s : archives) {
            s.close();
        }
    }

    private MemoizedValue<@Nullable ClassNode> lazyParse(@Nullable ClassData data) {
        return data == null ? obtainedNull() : memoize(() -> eagerParse(data));
    }

    private @Nullable ClassNode eagerParse(@Nullable ClassData data) {
        return data == null ? null : failWithRuntimeException(() -> parseClass(new ClassReader(data.read())));
    }
}
