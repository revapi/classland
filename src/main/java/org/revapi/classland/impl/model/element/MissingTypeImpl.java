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

import static java.util.Collections.emptyList;

import static org.revapi.classland.impl.util.Memoized.obtained;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.type.TypeKind;

import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.ErrorTypeImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;
import org.revapi.classland.impl.util.Packages;

public class MissingTypeImpl extends TypeElementBase {
    private final ErrorTypeImpl type;
    private final NameImpl qualifiedName;
    private final NameImpl simpleName;

    public MissingTypeImpl(Universe universe, String internalName) {
        super(universe, internalName, Memoized.memoize(() -> {
            String packageName = Packages.getPackageNameFromInternalName(internalName);
            return universe.getPackage(packageName);
        }), AnnotationSource.MEMOIZED_EMPTY);
        type = new ErrorTypeImpl(universe, this, null, emptyList(), this.annos);

        qualifiedName = NameImpl.of(internalName.replace('/', '.'));
        int lastSlash = internalName.lastIndexOf('/');
        simpleName = lastSlash == -1 ? qualifiedName : NameImpl.of(internalName.substring(lastSlash + 1));
    }

    @Override
    public DeclaredTypeImpl asType() {
        return type;
    }

    @Override
    public @Nullable ExecutableElementImpl getMethod(String methodName, String methodDescriptor) {
        return null;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.CLASS;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public NameImpl getSimpleName() {
        return simpleName;
    }

    @Override
    public TypeMirrorImpl getSuperclass() {
        return new NoTypeImpl(universe, obtained(emptyList()), TypeKind.NONE);
    }

    @Override
    public List<TypeMirrorImpl> getInterfaces() {
        return emptyList();
    }

    @Override
    public List<TypeParameterElementImpl> getTypeParameters() {
        return emptyList();
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return pkg.get();
    }

    @Override
    public List<ElementImpl> getEnclosedElements() {
        return emptyList();
    }

    @Override
    public NestingKind getNestingKind() {
        return NestingKind.TOP_LEVEL;
    }

    @Override
    public NameImpl getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitType(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return emptyList();
    }
}
