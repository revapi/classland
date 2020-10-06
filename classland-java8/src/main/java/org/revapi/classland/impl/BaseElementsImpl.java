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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor8;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class BaseElementsImpl implements Elements {
    private final Universe universe;
    private final Map<String, PackageElement> crossModulePackageSearchResults = new HashMap<>();
    private final Map<String, TypeElement> crossModuleTypeSearchResults = new HashMap<>();
    private final ElementVisitor<PackageElement, Void> packageByElement = new SimpleElementVisitor8<PackageElement, Void>() {

        @Override
        protected PackageElement defaultAction(Element e, Void ignored) {
            return visit(e.getEnclosingElement());
        }

        @Override
        public PackageElement visitPackage(PackageElement e, Void ignored) {
            return e;
        }

        @Override
        public PackageElement visitUnknown(Element e, Void aVoid) {
            return null;
        }
    };

    protected BaseElementsImpl(Universe universe) {
        this.universe = universe;
    }

    @Override
    public PackageElement getPackageElement(CharSequence name) {
        String key = name.toString();
        return crossModulePackageSearchResults.computeIfAbsent(key,
                __ -> universe.getModules().stream().flatMap(m -> m.getMutablePackages().values().stream())
                        .filter(p -> p.getSimpleName().contentEquals(name)).findFirst().orElse(null));
    }

    @Override
    public TypeElement getTypeElement(CharSequence name) {
        String key = name.toString();
        return crossModuleTypeSearchResults.computeIfAbsent(key,
                __ -> universe.getModules().stream().flatMap(m -> m.getMutablePackages().values().stream())
                        .flatMap(p -> p.getMutableTypes().values().stream())
                        .filter(t -> key.contentEquals(t.getQualifiedName()))
                        .findFirst()
                        .orElse(null));
    }

    @Override
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
            AnnotationMirror a) {
        // TODO implement
        return null;
    }

    @Override
    public String getDocComment(Element e) {
        return null;
    }

    @Override
    public boolean isDeprecated(Element e) {
        // TODO implement
        return false;
    }

    @Override
    public Name getBinaryName(TypeElement type) {
        // TODO implement
        return null;
    }

    @Override
    public PackageElement getPackageOf(Element type) {
        return packageByElement.visit(type);
    }

    @Override
    public List<? extends Element> getAllMembers(TypeElement type) {
        // TODO implement
        return null;
    }

    @Override
    public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
        // TODO implement
        return null;
    }

    @Override
    public boolean hides(Element hider, Element hidden) {
        // TODO implement
        return false;
    }

    @Override
    public boolean overrides(ExecutableElement overrider, ExecutableElement overridden, TypeElement type) {
        // TODO implement
        return false;
    }

    @Override
    public String getConstantExpression(Object value) {
        // TODO implement
        return null;
    }

    @Override
    public void printElements(Writer w, Element... elements) {
        // TODO implement
    }

    @Override
    public Name getName(CharSequence cs) {
        // TODO implement
        return null;
    }

    @Override
    public boolean isFunctionalInterface(TypeElement type) {
        // TODO implement
        return false;
    }
}
