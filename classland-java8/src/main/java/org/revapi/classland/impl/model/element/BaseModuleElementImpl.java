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

import static java.util.Collections.emptySet;

import static org.revapi.classland.impl.util.Memoized.memoize;
import static org.revapi.classland.impl.util.Memoized.obtained;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleNode;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

abstract class BaseModuleElementImpl extends ElementImpl {
    protected final @Nullable ModuleNode module;
    protected final NameImpl name;
    protected final Memoized<List<AnnotationMirrorImpl>> annos;
    protected final Memoized<NoTypeImpl> type;
    protected final Memoized<List<PackageElementImpl>> packages;

    private final Map<String, PackageElementImpl> mutablePackages = new ConcurrentHashMap<>();

    protected BaseModuleElementImpl(Universe universe, @Nullable ClassNode moduleType, TypeKind actualKind) {
        super(universe, obtained(moduleType == null ? AnnotationSource.EMPTY : AnnotationSource.fromType(moduleType)),
                AnnotationTargetPath.ROOT,
                // a roundabout way to "this" before we initialize it
                memoize(() -> moduleType == null ? universe.getUnnamedModule()
                        : universe.getModule(moduleType.module.name)));

        this.module = moduleType == null ? null : moduleType.module;
        this.name = NameImpl.of(module == null ? null : module.name);

        // ok, this is super ugly, but because we're only using this class as a means to share code between two impls
        // of ModuleElementImpl for the different java versions, let's be just be this courageous...
        ModuleElementImpl castThis = (ModuleElementImpl) this;

        this.type = memoize(
                () -> new NoTypeImpl(universe, memoize(() -> parseAnnotations(moduleType, castThis)), actualKind));
        this.packages = memoize(() -> new ArrayList<>(mutablePackages.values()));

        this.annos = memoize(() -> parseAnnotations(moduleType, castThis));
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
    public Name getSimpleName() {
        return getQualifiedName();
    }

    @Override
    public Element getEnclosingElement() {
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

        ModuleElementImpl getDependency();
    }
}