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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;
import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleNode;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public abstract class BaseModuleElementImpl extends ElementImpl {
    protected final @Nullable Archive archive;
    protected final @Nullable ModuleNode module;
    protected final NameImpl name;
    protected final MemoizedValue<List<AnnotationMirrorImpl>> annos;
    protected final MemoizedValue<NoTypeImpl> type;
    protected final MemoizedValue<List<PackageElementImpl>> packages;

    private final List<Consumer<Map<String, PackageElementImpl>>> packageGatherers;
    private final MemoizedValue<Map<String, PackageElementImpl>> gatheredPackages;
    private final Map<String, PackageElementImpl> mutablePackages = new ConcurrentHashMap<>();

    protected BaseModuleElementImpl(TypeLookup lookup, @Nullable Archive archive, @Nullable ClassNode moduleType,
            TypeKind actualKind) {
        super(lookup, obtained(moduleType == null ? AnnotationSource.EMPTY : AnnotationSource.fromType(moduleType)),
                AnnotationTargetPath.ROOT,
                // a roundabout way to "this" before we initialize it
                memoize(() -> moduleType == null ? lookup.getUnnamedModule()
                        : lookup.getModule(moduleType.module.name)));
        this.archive = archive;
        this.module = moduleType == null ? null : moduleType.module;
        this.name = NameImpl.of(module == null ? null : module.name);

        // ok, this is super ugly, but because we're only using this class as a means to share code between two impls
        // of ModuleElementImpl for the different java versions, let's be just be this courageous...
        ModuleElementImpl castThis = (ModuleElementImpl) this;

        this.annos = moduleType == null ? obtained(emptyList()) : memoize(() -> parseAnnotations(moduleType, castThis));

        this.type = memoize(() -> new NoTypeImpl(lookup, annos, actualKind));

        this.packageGatherers = new ArrayList<>();
        this.gatheredPackages = initGatheredPackages(packageGatherers);
        this.packages = gatheredPackages.map(m -> new ArrayList<>(m.values()));
    }

    protected BaseModuleElementImpl(TypeLookup lookup, @Nullable Archive archive, String moduleName,
            TypeKind actualKind) {
        super(lookup, AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, obtainedNull());
        this.archive = archive;
        this.module = null;
        this.name = NameImpl.of(moduleName);
        this.annos = obtained(emptyList());
        this.type = memoize(() -> new NoTypeImpl(lookup, annos, actualKind));
        this.packageGatherers = new ArrayList<>();
        this.gatheredPackages = initGatheredPackages(packageGatherers);
        this.packages = gatheredPackages.map(m -> new ArrayList<>(m.values()));
    }

    private static MemoizedValue<Map<String, PackageElementImpl>> initGatheredPackages(
            List<Consumer<Map<String, PackageElementImpl>>> gatherers) {
        return memoize(() -> {
            Map<String, PackageElementImpl> packages = new HashMap<>();
            gatherers.forEach(c -> c.accept(packages));
            return packages;
        });
    }

    public @Nullable Archive getArchive() {
        return archive;
    }

    public void addPackageGatherer(Consumer<Map<String, PackageElementImpl>> gatherer) {
        this.packageGatherers.add(gatherer);
    }

    public MemoizedValue<Map<String, PackageElementImpl>> computePackages() {
        return gatheredPackages;
    }

    public Map<String, PackageElementImpl> getMutablePackages() {
        return mutablePackages;
    }

    public Stream<ReachableModule> getReachableModules() {
        return Stream.empty();
    }

    @Override
    public TypeMirrorImpl asType() {
        return type.get();
    }

    public NameImpl getQualifiedName() {
        return name;
    }

    public ElementKind getKind() {
        return ElementKind.OTHER;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return emptySet();
    }

    @Override
    public NameImpl getSimpleName() {
        return getQualifiedName();
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return null;
    }

    @Override
    public List<PackageElementImpl> getEnclosedElements() {
        return packages.get();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitUnknown(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return annos.get();
    }

    public interface ReachableModule {
        boolean isTransitive();

        String getModuleName();
    }
}
