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
package org.revapi.classland.impl.model;

import static java.util.Collections.newSetFromMap;

import static org.revapi.classland.impl.util.ByteCode.parseClass;
import static org.revapi.classland.impl.util.Exceptions.callWithRuntimeException;
import static org.revapi.classland.impl.util.Exceptions.failWithRuntimeException;
import static org.revapi.classland.impl.util.Memoized.memoize;
import static org.revapi.classland.impl.util.Packages.getPackageNameFromInternalName;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.ErrorTypeImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.module.ClassData;
import org.revapi.classland.module.ModuleSource;

public final class Universe implements AutoCloseable {
    private final Set<ModuleSource> moduleSources = newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, PackageElementImpl> packages = new ConcurrentHashMap<>();
    private final Set<ModuleElementImpl> modules = newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, TypeElementImpl> typesByInternalName = new ConcurrentHashMap<>();

    public ElementsImpl getElements() {
        return new ElementsImpl(this);
    }

    public TypesImpl getTypes() {
        return new TypesImpl(this);
    }

    public Set<ModuleElementImpl> getModules() {
        return modules;
    }

    public Set<String> getPackages() {
        return packages.keySet();
    }

    public PackageElementImpl getPackage(String name) {
        return packages.get(name);
    }

    public Stream<PackageElementImpl> computePackagesForModule(ModuleElementImpl module) {
        return packages.values().stream().filter(pkg -> module.equals(pkg.getEnclosingElement()));
    }

    public Stream<TypeElementImpl> computeTypesForPackage(PackageElementImpl pkg) {
        return typesByInternalName.values().stream().filter(t -> t.isInPackage(pkg));
    }

    public Optional<TypeElementImpl> getTypeByInternalName(String internalName) {
        return Optional.ofNullable(typesByInternalName.get(internalName));
    }

    public DeclaredTypeImpl getDeclaredTypeByInternalName(String internalName) {
        return getTypeByInternalName(internalName).map(TypeElementImpl::asType).orElse(new ErrorTypeImpl(this));
    }

    public void registerModule(ModuleSource source) {
        moduleSources.add(source);
        ModuleContents contents = new ModuleContents(source);
        ModuleElementImpl module = contents.getModule().map(cd -> {
            ClassNode cls = failWithRuntimeException(() -> parseClass(new ClassReader(cd.read())));
            ModuleElementImpl m = new ModuleElementImpl(this, cls);
            modules.add(m);
            return m;
        }).orElse(null);

        contents.getPackages().forEach((name, data) -> {
            Memoized<List<AnnotationMirrorImpl>> annos = data == null ? memoize(Collections::emptyList)
                    : memoize(() -> parseAnnotations(data));
            packages.put(name, new PackageElementImpl(this, name, annos, module));
        });

        contents.getTypes().forEach(cd -> {
            String pkgName = getPackageNameFromInternalName(cd.getName());
            typesByInternalName.put(cd.getName(),
                    new TypeElementImpl(this, cd.getName(),
                            memoize(callWithRuntimeException(() -> parseClass(new ClassReader(cd.read())))),
                            getPackage(pkgName)));
        });
    }

    @Override
    public void close() throws Exception {
        for (ModuleSource s : moduleSources) {
            s.close();
        }
    }

    private List<AnnotationMirrorImpl> parseAnnotations(ClassData cd) {
        ClassNode cls = failWithRuntimeException(() -> parseClass(new ClassReader(cd.read())));
        return AnnotatedConstructImpl.parseAnnotations(this, cls);
    }

}
