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

import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeKind;

import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public final class PackageElementImpl extends ElementImpl implements PackageElement {
    private final NameImpl name;
    private final @Nullable ModuleElementImpl module;
    private final NoTypeImpl type;
    private final MemoizedValue<List<TypeElementImpl>> types;

    private final Map<String, TypeElementImpl> mutableTypes = new ConcurrentHashMap<>();

    public PackageElementImpl(Universe universe, String name, MemoizedValue<@Nullable ClassNode> node,
            @Nullable ModuleElementImpl module) {
        super(universe, node.map(n -> n == null ? AnnotationSource.EMPTY : AnnotationSource.fromType(n)),
                AnnotationTargetPath.ROOT, obtained(module));
        this.name = NameImpl.of(name);
        this.module = module;
        this.type = new NoTypeImpl(universe, this.annos, TypeKind.PACKAGE);
        this.types = memoize(() -> new ArrayList<>(mutableTypes.values()));
    }

    public @Nullable ModuleElementImpl getModule() {
        return module;
    }

    public Map<String, TypeElementImpl> getMutableTypes() {
        return mutableTypes;
    }

    @Override
    public NameImpl getQualifiedName() {
        return name;
    }

    @Override
    public boolean isUnnamed() {
        return name.contentEquals("");
    }

    @Override
    public TypeMirrorImpl asType() {
        return type;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.PACKAGE;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return emptySet();
    }

    @Override
    public NameImpl getSimpleName() {
        return name;
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return module;
    }

    @Override
    public List<? extends ElementImpl> getEnclosedElements() {
        return types.get();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitPackage(this, p);
    }
}
