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

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import org.revapi.classland.impl.model.element.ModuleElementImpl;

public class ElementsImpl extends BaseElementsImpl implements Elements {
    public ElementsImpl(Universe universe) {
        super(universe);
    }

    @Override
    public PackageElement getPackageElement(ModuleElement module, CharSequence name) {
        return ((ModuleElementImpl) module).getMutablePackages().get(name.toString());
    }

    @Override
    public Set<? extends PackageElement> getAllPackageElements(CharSequence name) {
        return new HashSet<>(crossModulePackagesByName.apply(name).values());
    }

    @Override
    public TypeElement getTypeElement(ModuleElement module, CharSequence name) {
        return ((ModuleElementImpl) module).getMutablePackages().values().stream()
                .flatMap(p -> p.getMutableTypes().values().stream())
                .filter(t -> t.getQualifiedName().contentEquals(name)).findFirst().orElse(null);
    }

    @Override
    public Set<? extends TypeElement> getAllTypeElements(CharSequence name) {
        return new HashSet<>(crossModuleTypesByFqn.apply(name).values());
    }

    @Override
    public ModuleElement getModuleElement(CharSequence name) {
        return universe.getModule(name.toString());
    }

    @Override
    public Set<? extends ModuleElement> getAllModuleElements() {
        return new HashSet<>(universe.getModules());
    }

    @Override
    public Origin getOrigin(Element e) {
        return Origin.EXPLICIT;
    }

    @Override
    public Origin getOrigin(AnnotatedConstruct c, AnnotationMirror a) {
        return Origin.EXPLICIT;
    }

    @Override
    public Origin getOrigin(ModuleElement m, ModuleElement.Directive directive) {
        return Origin.EXPLICIT;
    }

    @Override
    public boolean isBridge(ExecutableElement e) {
        return false;
    }

    @Override
    public ModuleElement getModuleOf(Element e) {
        while (!(e instanceof ModuleElement)) {
            e = e.getEnclosingElement();
        }
        return (ModuleElement) e;
    }

    @Override
    public PackageElement getPackageOf(Element type) {
        if (type instanceof ModuleElement) {
            return null;
        } else {
            return super.getPackageOf(type);
        }
    }
}
