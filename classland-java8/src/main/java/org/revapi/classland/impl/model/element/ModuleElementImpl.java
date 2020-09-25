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
import static java.util.stream.Collectors.toList;

import static org.objectweb.asm.Opcodes.ACC_OPEN;
import static org.revapi.classland.impl.util.Memoized.memoize;
import static org.revapi.classland.impl.util.Memoized.obtained;

import java.util.List;
import java.util.Set;

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
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.Memoized;

public final class ModuleElementImpl extends ElementImpl {
    private final NameImpl name;
    private final Memoized<List<AnnotationMirrorImpl>> annos;
    private final Memoized<NoTypeImpl> type;
    private final Memoized<List<PackageElementImpl>> packages;

    public ModuleElementImpl(Universe universe, ClassNode moduleType) {
        super(universe, obtained(AnnotationSource.fromType(moduleType)));
        this.name = NameImpl.of(moduleType.module.name);
        this.type = memoize(
                () -> new NoTypeImpl(universe, memoize(() -> parseAnnotations(moduleType)), TypeKind.OTHER));
        this.packages = memoize(() -> universe.computePackagesForModule(this).collect(toList()));

        this.annos = memoize(() -> parseAnnotations(moduleType));
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
}
