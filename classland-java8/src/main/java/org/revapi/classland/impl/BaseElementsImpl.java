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

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import org.revapi.classland.PrettyPrinting;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.MissingTypeImpl;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.mirror.ExecutableTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.MemoizedFunction;

abstract class BaseElementsImpl implements Elements {
    protected final Universe universe;
    protected final MemoizedFunction<CharSequence, Map<ModuleElementImpl, PackageElement>> crossModulePackagesByName;
    protected final MemoizedFunction<CharSequence, Map<ModuleElementImpl, TypeElement>> crossModuleTypesByFqn;

    protected BaseElementsImpl(Universe universe) {
        this.universe = universe;

        this.crossModulePackagesByName = MemoizedFunction.memoize(name -> {
            Map<ModuleElementImpl, PackageElement> ret = new HashMap<>();
            for (ModuleElementImpl m : universe.getModules()) {
                for (Map.Entry<String, PackageElementImpl> e : m.getMutablePackages().entrySet()) {
                    if (e.getKey().contentEquals(name)) {
                        ret.put(m, e.getValue());
                        break;
                    }
                }
            }

            return ret;
        });

        this.crossModuleTypesByFqn = MemoizedFunction.memoize(name -> {
            Map<ModuleElementImpl, TypeElement> ret = new HashMap<>();
            String typeName = name.toString();
            for (ModuleElementImpl m : universe.getModules()) {
                pkgs: for (Map.Entry<String, PackageElementImpl> e : m.getMutablePackages().entrySet()) {
                    if (typeName.startsWith(e.getKey())) {
                        for (TypeElement t : e.getValue().getMutableTypes().values()) {
                            if (typeName.contentEquals(t.getQualifiedName())) {
                                ret.put(m, t);
                                break pkgs;
                            }
                        }
                    }
                }
            }

            return ret;
        });
    }

    @Override
    public PackageElement getPackageElement(CharSequence name) {
        Map<ModuleElementImpl, PackageElement> allPackages = crossModulePackagesByName.apply(name);
        return allPackages.isEmpty() ? null : allPackages.values().iterator().next();
    }

    @Override
    public TypeElement getTypeElement(CharSequence name) {
        Map<ModuleElementImpl, TypeElement> allTypes = crossModuleTypesByFqn.apply(name);
        return allTypes.size() != 1 ? null : allTypes.values().iterator().next();
    }

    @Override
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
            AnnotationMirror a) {
        Map<ExecutableElement, AnnotationValue> ret = new HashMap<>(a.getElementValues());
        TypeElement t = TypeUtils.asTypeElement(a.getAnnotationType());

        for (ExecutableElement e : ElementFilter.methodsIn(t.getEnclosedElements())) {
            AnnotationValue v = e.getDefaultValue();
            if (v != null && !ret.containsKey(e)) {
                ret.put(e, v);
            }
        }

        return ret;
    }

    @Override
    public String getDocComment(Element e) {
        return null;
    }

    @Override
    public boolean isDeprecated(Element e) {
        return ((ElementImpl) e).isDeprecated();
    }

    @Override
    public Name getBinaryName(TypeElement type) {
        return NameImpl.of(((TypeElementBase) type).getInternalName().replace('/', '.'));
    }

    @Override
    public PackageElement getPackageOf(Element type) {
        return TypeUtils.getPackageOf(type);
    }

    @Override
    public List<? extends Element> getAllMembers(TypeElement type) {
        List<Element> ret = new ArrayList<>(type.getEnclosedElements());
        for (TypeElement superType : getAllSuperTypes(type)) {
            superType.getEnclosedElements().stream()
                    .filter(e -> !e.getModifiers().contains(Modifier.PRIVATE))
                    .filter(e -> e.getKind() != ElementKind.CONSTRUCTOR)
                    .forEach(ret::add);
        }

        return ret;
    }

    @Override
    public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
        List<AnnotationMirror> ret = new ArrayList<>(e.getAnnotationMirrors());
        while (e instanceof TypeElementImpl) {
            TypeMirrorImpl superClassType = ((TypeElementImpl) e).getSuperclass();
            if (superClassType.getKind() == TypeKind.ERROR) {
                break;
            }

            e = TypeUtils.asTypeElement(superClassType);

            if (e == universe.getJavaLangObject()) {
                break;
            }

            List<? extends AnnotationMirror> annos = e.getAnnotationMirrors();
            for (AnnotationMirror a : annos) {
                if (isInherited(a.getAnnotationType().asElement()) && !containsAnnotationOfType(ret, a)) {
                    ret.add(a);
                }
            }
        }

        return ret;
    }

    @Override
    public boolean hides(Element hider, Element hidden) {
        // different element kinds live in different namespaces
        if (hider.getKind() != hidden.getKind()) {
            return false;
        }

        if (!hider.getSimpleName().contentEquals(hidden.getSimpleName())) {
            return false;
        }

        if (hider.getKind() == ElementKind.METHOD) {
            if (!hider.getModifiers().contains(Modifier.STATIC)) {
                // only static methods hide... instance methods overload
                return false;
            }

            if (!TypeUtils.isSubSignature((ExecutableTypeImpl) hider.asType(), (ExecutableTypeImpl) hidden.asType())) {
                return false;
            }
        }

        // hider must be in a subclass of hidden
        TypeElement hiderType = TypeUtils.getNearestType(hider);
        TypeElement hiddenType = TypeUtils.getNearestType(hidden);

        if (hiderType == null || hiddenType == null) {
            // this really should not happen actually
            return false;
        }

        if (!TypeUtils.isSubclass(hiderType, hiddenType)) {
            return false;
        }

        // hidden must be accessible in the hider's class
        return TypeUtils.isAccessibleIn(hidden, hiderType);
    }

    @Override
    public boolean overrides(ExecutableElement overrider, ExecutableElement overridden, TypeElement type) {
        if (overrider == overridden) {
            return false;
        }

        if (!overrider.getSimpleName().contentEquals(overridden.getSimpleName())) {
            return false;
        }

        if (overrider.getModifiers().contains(Modifier.STATIC)) {
            // static methods don't override
            return false;
        }

        if (!TypeUtils.isMemberOf(overridden, type)) {
            return false;
        }

        return TypeUtils.overrides(overrider, overridden, type);
    }

    @Override
    public String getConstantExpression(Object value) {
        return PrettyPrinting.printConstant(new StringWriter(), value).toString();
    }

    @Override
    public void printElements(Writer w, Element... elements) {
        for (Element e : elements) {
            PrettyPrinting.print(w, e);
        }
    }

    @Override
    public Name getName(CharSequence cs) {
        return NameImpl.of(cs.toString());
    }

    @Override
    public boolean isFunctionalInterface(TypeElement type) {
        // TODO implement
        return false;
    }

    private boolean isInherited(Element annotationElement) {
        for (AnnotationMirror a : annotationElement.getAnnotationMirrors()) {
            Name annoTypeName = TypeUtils.asTypeElement(a.getAnnotationType()).getQualifiedName();
            if ("java.lang.annotation.Inherited".contentEquals(annoTypeName)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsAnnotationOfType(List<AnnotationMirror> annos, AnnotationMirror am) {
        Element annoType = am.getAnnotationType().asElement();
        for (AnnotationMirror a : annos) {
            if (a.getAnnotationType().asElement() == annoType) {
                return true;
            }
        }

        return false;
    }

    private Set<TypeElement> getAllSuperTypes(TypeElement type) {
        HashSet<TypeElement> ret = new HashSet<>(4);
        fillAllSuperTypes(type, ret);
        return ret;
    }

    private void fillAllSuperTypes(TypeElement type, Set<TypeElement> superTypes) {
        if (type instanceof MissingTypeImpl) {
            superTypes.add(universe.getJavaLangObject());
        } else {
            TypeMirror superClassType = type.getSuperclass();
            TypeElement superClass = superClassType == null ? null : TypeUtils.asTypeElement(superClassType);
            if (superClass != null) {
                superTypes.add(superClass);
                fillAllSuperTypes(superClass, superTypes);
            }

            for (TypeMirror t : type.getInterfaces()) {
                TypeElement te = TypeUtils.asTypeElement(t);
                if (te != null) {
                    superTypes.add(te);
                    fillAllSuperTypes(te, superTypes);
                }
            }
        }
    }
}
