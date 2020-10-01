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
package org.revapi.classland.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.newSetFromMap;

import static org.revapi.classland.impl.util.ByteCode.parseClass;
import static org.revapi.classland.impl.util.Exceptions.failWithRuntimeException;
import static org.revapi.classland.impl.util.Memoized.memoize;
import static org.revapi.classland.impl.util.Memoized.obtained;
import static org.revapi.classland.impl.util.Packages.getPackageNameFromInternalName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;
import org.revapi.classland.impl.model.element.MissingTypeImpl;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.element.UnnamedModuleImpl;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

public final class Universe implements AutoCloseable {
    public static final TypeSignature.Reference JAVA_LANG_OBJECT_SIG = new TypeSignature.Reference(0,
            "java/lang/Object", emptyList(), null);

    private final UnnamedModuleImpl unnamedModule = new UnnamedModuleImpl(this);
    private final boolean analyzeModules;
    private final Set<Archive> archives = newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, ModuleElementImpl> modules = new ConcurrentHashMap<>();
    private final Map<String, Map<ModuleElementImpl, TypeElementImpl>> typesByNameAndModule = new ConcurrentHashMap<>();

    public Universe(boolean analyzeModules) {
        this.analyzeModules = analyzeModules;
    }

    public @Nullable ModuleElementImpl getModule(String name) {
        return modules.get(name);
    }

    public UnnamedModuleImpl getUnnamedModule() {
        return unnamedModule;
    }

    public Collection<ModuleElementImpl> getModules() {
        return modules.values();
    }

    public Collection<PackageElementImpl> getPackagesForModule(ModuleElementImpl module) {
        return module.getMutablePackages().values();
    }

    public @Nullable PackageElementImpl getPackageInModule(String packageName, @Nullable ModuleElementImpl module) {
        return module == null ? null : module.getMutablePackages().get(packageName);
    }

    public TypeElementBase getTypeByInternalNameFromPackage(String internalName, PackageElementImpl startingPackage) {
        return getTypeByInternalNameFromModule(internalName, startingPackage.getModule());
    }

    public TypeElementBase getTypeByInternalNameFromModule(String internalName,
            @Nullable ModuleElementImpl startingModule) {
        Map<ModuleElementImpl, TypeElementImpl> types = typesByNameAndModule.get(internalName);
        if (types == null) {
            return new MissingTypeImpl(this, internalName, startingModule);
        }

        ModuleElementImpl actualModule = startingModule == null ? unnamedModule : startingModule;

        TypeElementImpl t = types.get(actualModule);
        if (t != null) {
            return t;
        }

        Iterator<ModuleElementImpl> reachableModules = new Iterator<ModuleElementImpl>() {
            final List<ModuleElementImpl.ReachableModule> nexts = new ArrayList<>();
            {
                actualModule.getReachableModules().forEach(nexts::add);
            }

            @Override
            public boolean hasNext() {
                return !nexts.isEmpty();
            }

            @Override
            public ModuleElementImpl next() {
                if (nexts.isEmpty()) {
                    throw new NoSuchElementException();
                }
                ModuleElementImpl.ReachableModule m = nexts.remove(0);
                if (m.isTransitive()) {
                    m.getDependency().getReachableModules().forEach(nexts::add);
                }
                return m.getDependency();
            }
        };

        while (reachableModules.hasNext()) {
            ModuleElementImpl m = reachableModules.next();
            t = types.get(m);
            if (t != null) {
                return t;
            }
        }

        return new MissingTypeImpl(this, internalName, startingModule);
    }

    public void registerArchive(Archive source) {
        archives.add(source);
        ArchiveContents contents = new ArchiveContents(source);
        ModuleElementImpl module = contents.getModule().map(cd -> {
            if (!analyzeModules) {
                return unnamedModule;
            }
            ClassNode cls = eagerParse(cd);
            ModuleElementImpl m = new ModuleElementImpl(this, cls);
            modules.put(m.getQualifiedName().toString(), m);
            return m;
        }).orElse(unnamedModule);

        contents.getPackages().forEach((name, data) -> {
            module.getMutablePackages().put(name,
                    new PackageElementImpl(this, name, lazyParse(data), analyzeModules ? module : null));
        });

        contents.getTypes().forEach(cd -> {
            String pkgName = getPackageNameFromInternalName(cd.getName());
            PackageElementImpl pkg = module.getMutablePackages().get(pkgName);
            TypeElementImpl type = new TypeElementImpl(this, cd.getName(), lazyParse(cd), pkg);
            pkg.getMutableTypes().put(cd.getName(), type);
            typesByNameAndModule.computeIfAbsent(cd.getName(), __ -> new ConcurrentHashMap<>()).put(module, type);
        });
    }

    @Override
    public void close() throws Exception {
        for (Archive s : archives) {
            s.close();
        }
    }

    private Memoized<@Nullable ClassNode> lazyParse(@Nullable ClassData data) {
        return data == null ? obtained(null) : memoize(() -> eagerParse(data));
    }

    private @Nullable ClassNode eagerParse(@Nullable ClassData data) {
        return data == null ? null : failWithRuntimeException(() -> parseClass(new ClassReader(data.read())));
    }
}
