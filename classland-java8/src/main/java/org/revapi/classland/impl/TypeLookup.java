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

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import static org.revapi.classland.impl.util.MemoizedValue.obtained;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.MissingTypeImpl;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.NoElementImpl;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.element.TypeParameterElementImpl;
import org.revapi.classland.impl.model.mirror.NullTypeImpl;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.MemoizedBiFunction;
import org.revapi.classland.impl.util.MemoizedFunction;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public final class TypeLookup implements AutoCloseable {
    public static final TypeSignature.Reference JAVA_LANG_OBJECT_SIG = new TypeSignature.Reference(0,
            "java/lang/Object", emptyList(), null);

    public final NullTypeImpl nullType = new NullTypeImpl(this);

    private final TypePool universe;
    private final MemoizedBiFunction<String, @Nullable ModuleElementImpl, TypeElementBase> getTypeByInternalNameFromModule;
    private final MemoizedFunction<String, ModuleElementImpl> getModule;
    private final MemoizedBiFunction<String, @Nullable ModuleElementImpl, PackageElementImpl> getPackageInModule;
    private final MemoizedBiFunction<String, PackageElementImpl, TypeElementBase> getTypeByInternalNameFromPackage;
    private final MemoizedValue<Collection<ModuleElementImpl>> getModules;
    private final MemoizedValue<TypeElementBase> getJavaLangObject;
    private final MemoizedValue<TypeElementBase> getJavaLangCloneable;
    private final MemoizedValue<TypeElementBase> getJavaIoSerializable;
    private final MemoizedValue<ModuleElementImpl> getJavaBase;

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
            return obtained(getUnnamedModule());
        }

        @Override
        public ElementImpl asElement() {
            return new NoElementImpl(TypeLookup.this);
        }
    };

    public TypeLookup(TypePool universe) {
        this.universe = universe;
        MemoizedFunction<ModuleElementImpl, Map<String, TypeElementImpl>> typesInModule = MemoizedFunction
                .memoize(m -> m.computePackages().get().values().stream().flatMap(p -> p.computeTypes().get().stream())
                        .collect(toMap(TypeElementBase::getInternalName, identity())));

        getTypeByInternalNameFromModule = MemoizedBiFunction.memoize((internalName, startingModule) -> {
            ModuleElementImpl actualModule = startingModule == null ? getUnnamedModule() : startingModule;

            TypeElementBase type = typesInModule.apply(actualModule).get(internalName);
            if (type == null) {
                ReachableModulesIterator reachableModules = new ReachableModulesIterator(TypeLookup.this, actualModule);
                while (reachableModules.hasNext()) {
                    ModuleElementImpl m = reachableModules.next();
                    type = typesInModule.apply(m).get(internalName);
                    if (type != null) {
                        return type;
                    }
                }

                type = new MissingTypeImpl(TypeLookup.this, internalName, startingModule);
            }

            return type;
        });

        getModule = MemoizedFunction.memoize(universe::getModule);
        getPackageInModule = MemoizedBiFunction
                .memoize((pkg, m) -> m == null ? null : m.computePackages().get().get(pkg));
        getTypeByInternalNameFromPackage = MemoizedBiFunction
                .memoize((internalName, pkg) -> getTypeByInternalNameFromModule.apply(internalName, pkg.getModule()));

        getModules = MemoizedValue.memoize(universe::getModules);
        getJavaBase = MemoizedValue.memoize(universe::getJavaBase);
        getJavaLangObject = getJavaBase.map(jb -> getTypeByInternalNameFromModule.apply("java/lang/Object", jb));
        getJavaLangCloneable = getJavaBase.map(jb -> getTypeByInternalNameFromModule.apply("java/lang/Cloneable", jb));
        getJavaIoSerializable = getJavaBase
                .map(jb -> getTypeByInternalNameFromModule.apply("java/io/Serializable", jb));
    }

    public TypeElementBase getTypeByInternalNameFromModule(String internalName,
            @Nullable ModuleElementImpl typeLookupSeed) {
        return getTypeByInternalNameFromModule.apply(internalName, typeLookupSeed);
    }

    public ModuleElementImpl getUnnamedModule() {
        return universe.getUnnamedModule();
    }

    public ModuleElementImpl getModule(String module) {
        return getModule.apply(module);
    }

    public PackageElementImpl getPackageInModule(String name, @Nullable ModuleElementImpl module) {
        return getPackageInModule.apply(name, module);
    }

    public TypeElementBase getTypeByInternalNameFromPackage(String internalName, PackageElementImpl pkg) {
        return getTypeByInternalNameFromPackage.apply(internalName, pkg);
    }

    public Collection<ModuleElementImpl> getModules() {
        return getModules.get();
    }

    public TypeElementBase getJavaLangObject() {
        return getJavaLangObject.get();
    }

    public TypeElementBase getJavaLangCloneable() {
        return getJavaLangCloneable.get();
    }

    public TypeElementBase getJavaIoSerializable() {
        return getJavaIoSerializable.get();
    }

    public ModuleElementImpl getJavaBase() {
        return getJavaBase.get();
    }

    @Override
    public void close() throws Exception {
        universe.close();
    }
}
