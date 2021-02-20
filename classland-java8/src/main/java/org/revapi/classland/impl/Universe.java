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
import static java.util.stream.Collectors.toList;

import static org.revapi.classland.impl.util.ByteCode.parseClass;
import static org.revapi.classland.impl.util.Exceptions.callWithRuntimeException;
import static org.revapi.classland.impl.util.Exceptions.failWithRuntimeException;
import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;
import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;
import static org.revapi.classland.impl.util.Packages.getPackageNameFromInternalName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;
import org.revapi.classland.archive.ModuleResolver;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.MissingTypeImpl;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.NoElementImpl;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.element.TypeParameterElementImpl;
import org.revapi.classland.impl.model.element.UnnamedModuleImpl;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public final class Universe implements AutoCloseable {
    public static final TypeSignature.Reference JAVA_LANG_OBJECT_SIG = new TypeSignature.Reference(0,
            "java/lang/Object", emptyList(), null);

    public final TypeVariableResolutionContext noTypeVariables = new TypeVariableResolutionContext() {
        @Override
        public Optional<TypeParameterElementImpl> resolveTypeVariable(String name) {
            return Optional.empty();
        }

        @Override
        public MemoizedValue<AnnotationSource> asAnnotationSource() {
            return AnnotationSource.MEMOIZED_EMPTY;
        }

        @Override
        public MemoizedValue<ModuleElementImpl> lookupModule() {
            return obtained(unnamedModule);
        }

        @Override
        public ElementImpl asElement() {
            return new NoElementImpl(Universe.this);
        }
    };

    private final UnnamedModuleImpl unnamedModule = new UnnamedModuleImpl(this);
    private final boolean analyzeModules;
    private final Set<Archive> archives = newSetFromMap(new ConcurrentHashMap<>());
    private final Set<ModuleResolver> moduleResolvers = new HashSet<>();
    private final Map<String, MemoizedValue<ModuleElementImpl>> modules = new ConcurrentHashMap<>();
    private final Map<String, Map<ModuleElementImpl, TypeElementImpl>> typesByNameAndModule = new ConcurrentHashMap<>();

    public Universe(boolean analyzeModules) {
        this.analyzeModules = analyzeModules;
        modules.put("", obtained(unnamedModule));
    }

    public MemoizedValue<@Nullable ModuleElementImpl> getModule(String name) {
        return modules.getOrDefault(name, obtainedNull());
    }

    public UnnamedModuleImpl getUnnamedModule() {
        return unnamedModule;
    }

    public Collection<ModuleElementImpl> getModules() {
        return modules.values().stream().map(MemoizedValue::get).collect(toList());
    }

    public Collection<PackageElementImpl> getPackagesForModule(ModuleElementImpl module) {
        return module.getMutablePackages().values();
    }

    public @Nullable PackageElementImpl getPackageInModule(String packageName, @Nullable ModuleElementImpl module) {
        return module == null ? null : module.getMutablePackages().get(packageName);
    }

    public TypeElementBase getJavaLangObject() {
        return getTypeByInternalNameFromModule("java/lang/Object", getJavaBase());
    }

    public ModuleElementImpl getJavaBase() {
        ModuleElementImpl javaBase = analyzeModules ? modules.get("java.base").get() : unnamedModule;
        if (javaBase == null) {
            javaBase = unnamedModule;
        }

        return javaBase;
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
            final Set<String> visited = new HashSet<>();
            final List<ModuleElementImpl.ReachableModule> nexts = new ArrayList<>();
            ModuleElementImpl nextModule;

            {
                actualModule.getReachableModules().forEach(nexts::add);
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

                ModuleElementImpl m = modules.getOrDefault(next, obtainedNull()).get();

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

    public void registerArchive(Archive archive) {
        archives.add(archive);
        ArchiveContents contents = new ArchiveContents(archive);
        ModuleElementImpl module = contents.getModule().map(cd -> {
            if (!analyzeModules) {
                return unnamedModule;
            }
            ClassNode cls = eagerParse(cd);
            ModuleElementImpl m = new ModuleElementImpl(this, cls);
            modules.put(m.getQualifiedName().toString(), obtained(m));
            return m;
        }).orElseGet(() -> contents.getModuleName().map(n -> new ModuleElementImpl(this, n)).orElse(unnamedModule));


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

        modules.put(module.getQualifiedName().toString(), obtained(module));
    }

    private ModuleElementImpl doRegisterArchive(Archive archive) {
        archives.add(archive);
        ArchiveContents contents = new ArchiveContents(archive);
        ModuleElementImpl module = contents.getModule().map(cd -> {
            if (!analyzeModules) {
                return unnamedModule;
            }
            ClassNode cls = eagerParse(cd);
            return new ModuleElementImpl(this, cls);
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

        return module;
    }

    public void addModule(String name) {
        modules.putIfAbsent(name, memoize(callWithRuntimeException(() -> {
            for (ModuleResolver r : moduleResolvers) {
                ModuleElementImpl m = r.getModuleArchive(name).map(this::doRegisterArchive).orElse(null);
                if (m != null) {
                    return m;
                }
            }

            return null;
        })));
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
                ModuleElementImpl m = modules.getOrDefault(moduleName, obtainedNull()).get();
                if (m == null) {
                    continue;
                }

                m.getReachableModules().forEach(reachable -> {
                    String dep = reachable.getModuleName();
                    modules.computeIfAbsent(dep, d -> {
                        try {
                            return obtained(findModule(d));
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to find the module '" + d
                                    + "' while computing the transitive closure of the reachable modules.", e);
                        }
                    });

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

    private @Nullable ModuleElementImpl findModule(String name) throws IOException {
        for (ModuleResolver r : moduleResolvers) {
            ModuleElementImpl m = r.getModuleArchive(name).map(this::doRegisterArchive).orElse(null);
            if (m != null) {
                return m;
            }
        }

        return null;
    }

    private MemoizedValue<@Nullable ClassNode> lazyParse(@Nullable ClassData data) {
        return data == null ? obtainedNull() : memoize(() -> eagerParse(data));
    }

    private @Nullable ClassNode eagerParse(@Nullable ClassData data) {
        return data == null ? null : failWithRuntimeException(() -> parseClass(new ClassReader(data.read())));
    }
}
