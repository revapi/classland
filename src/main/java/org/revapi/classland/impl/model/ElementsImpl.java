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

import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class ElementsImpl implements Elements {
    private final Universe universe;

    public ElementsImpl(Universe universe) {
        this.universe = universe;
    }

    @Override
    public PackageElement getPackageElement(CharSequence name) {
        return universe.getPackage(name.toString());
    }

    @Override
    public PackageElement getPackageElement(ModuleElement module, CharSequence name) {
        return null;
    }

    @Override
    public Set<? extends PackageElement> getAllPackageElements(CharSequence name) {
        return null;
    }

    @Override
    public TypeElement getTypeElement(CharSequence name) {
        return null;
    }

    @Override
    public TypeElement getTypeElement(ModuleElement module, CharSequence name) {
        return null;
    }

    @Override
    public Set<? extends TypeElement> getAllTypeElements(CharSequence name) {
        return null;
    }

    @Override
    public ModuleElement getModuleElement(CharSequence name) {
        return null;
    }

    @Override
    public Set<? extends ModuleElement> getAllModuleElements() {
        return universe.getModules();
    }

    @Override
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
            AnnotationMirror a) {
        return null;
    }

    @Override
    public String getDocComment(Element e) {
        return null;
    }

    @Override
    public boolean isDeprecated(Element e) {
        return false;
    }

    @Override
    public Origin getOrigin(Element e) {
        return null;
    }

    @Override
    public Origin getOrigin(AnnotatedConstruct c, AnnotationMirror a) {
        return null;
    }

    @Override
    public Origin getOrigin(ModuleElement m, ModuleElement.Directive directive) {
        return null;
    }

    @Override
    public boolean isBridge(ExecutableElement e) {
        return false;
    }

    @Override
    public Name getBinaryName(TypeElement type) {
        return null;
    }

    @Override
    public PackageElement getPackageOf(Element type) {
        return null;
    }

    @Override
    public ModuleElement getModuleOf(Element type) {
        return null;
    }

    @Override
    public List<? extends Element> getAllMembers(TypeElement type) {
        return null;
    }

    @Override
    public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
        return null;
    }

    @Override
    public boolean hides(Element hider, Element hidden) {
        return false;
    }

    @Override
    public boolean overrides(ExecutableElement overrider, ExecutableElement overridden, TypeElement type) {
        return false;
    }

    @Override
    public String getConstantExpression(Object value) {
        return null;
    }

    @Override
    public void printElements(Writer w, Element... elements) {

    }

    @Override
    public Name getName(CharSequence cs) {
        return null;
    }

    @Override
    public boolean isFunctionalInterface(TypeElement type) {
        return false;
    }
}
