/*
 * Copyright 2020-2021 Lukas Krejci
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

import static org.revapi.classland.impl.util.MemoizedFunction.memoize;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import org.revapi.classland.ClasslandElements;
import org.revapi.classland.PrettyPrinting;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.MissingTypeImpl;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.util.MemoizedFunction;
import org.revapi.classland.impl.util.Nullable;

abstract class BaseElementsImpl implements Elements, ClasslandElements {
    protected final Universe universe;
    protected final MemoizedFunction<CharSequence, Map<ModuleElementImpl, PackageElement>> crossModulePackagesByName;
    protected final MemoizedFunction<CharSequence, Map<ModuleElementImpl, TypeElement>> crossModuleTypesByFqn;
    protected final MemoizedFunction<TypeElement, Boolean> isFunctionalInterface;

    protected BaseElementsImpl(Universe universe) {
        this.universe = universe;

        this.crossModulePackagesByName = memoize(name -> {
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

        this.crossModuleTypesByFqn = memoize(name -> {
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

        WeakHashMap<TypeElement, Boolean> cache = new WeakHashMap<>();
        this.isFunctionalInterface = memoize(type -> {
            Boolean result = cache.get(type);
            if (result != null) {
                return result;
            }

            // TODO implement
            return false;
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
    public @Nullable TypeElement getTypeElementByBinaryName(String binaryName) {
        String internalName = binaryName.replace('.', '/');
        return universe.getTypeByInternalNameFromModule(internalName, null);
    }

    @Override
    public @Nullable TypeElement getTypeElementByBinaryName(String moduleName, String binaryName) {
        String internalName = binaryName.replace('.', '/');
        ModuleElementImpl module = universe.getModule(moduleName).get();
        if (module == null) {
            return null;
        }
        return universe.getTypeByInternalNameFromModule(internalName, module);
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
            superType.getEnclosedElements().stream().filter(e -> !e.getModifiers().contains(Modifier.PRIVATE))
                    .filter(e -> e.getKind() != ElementKind.CONSTRUCTOR).forEach(ret::add);
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
                    // javac prepends, so let's do the same thing
                    ret.add(0, a);
                }
            }
        }

        return ret;
    }

    @Override
    public boolean hides(Element hider, Element hidden) {
        if (hider == hidden) {
            return false;
        }

        // different element kinds live in different namespaces
        if (TypeUtils.ElementNamespace.of(hider.getKind()) != TypeUtils.ElementNamespace.of(hidden.getKind())) {
            return false;
        }

        if (!hider.getSimpleName().contentEquals(hidden.getSimpleName())) {
            return false;
        }

        if (hider.getKind() == ElementKind.METHOD) {
            if (!hider.getModifiers().contains(Modifier.STATIC)) {
                // only static methods hide... instance methods override
                return false;
            }

            if (!TypeUtils.isSubSignature((ExecutableType) hider.asType(), (ExecutableType) hidden.asType(),
                    universe)) {
                return false;
            }
        }

        // hider must be in a subclass of hidden
        TypeElement hiderType = TypeUtils.getNearestType(hider.getEnclosingElement());
        TypeElement hiddenType = TypeUtils.getNearestType(hidden.getEnclosingElement());

        if (hiderType == null && hiddenType == null) {
            // top level elements with the same name in the same namespace
            return true;
        }

        if (hiderType == null || hiddenType == null) {
            // one of the elements is top level, the other isn't.. they can't hide each other
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
        // 8.4.8.1. Overriding (by Instance Methods)
        //
        // An instance method mC declared in or inherited by class C, overrides from C another method mA declared in
        // class A, iff all of the following are true:
        // C is a subclass of A.
        // C does not inherit mA.
        // The signature of mC is a subsignature (§8.4.2) of the signature of mA as a member of the supertype of C that
        // names A.
        // One of the following is true:
        // mA is public.
        // mA is protected.
        // mA is declared with package access in the same package as C, and either C declares mC or mA is a member of
        // the direct superclass type of C.
        // mA is declared with package access and mC overrides mA from some superclass of C.
        // mA is declared with package access and mC overrides a method m' from C (m' distinct from mC and mA), such
        // that m' overrides mA from some superclass of C.
        // If mC is non-abstract and overrides from C an abstract method mA, then mC is said to implement mA from C.
        // It is a compile-time error if the overridden method, mA, is a static method.
        // In this respect, overriding of methods differs from hiding of fields (§8.3), for it is permissible for an
        // instance variable to hide a static variable.
        // An instance method mC declared in or inherited by class C, overrides from C another method mI declared in
        // interface I, iff all of the following are true:
        // I is a superinterface of C.
        // mI is not static.
        // C does not inherit mI.
        // The signature of mC is a subsignature (§8.4.2) of the signature of mI as a member of the supertype of C that
        // names I.
        // mI is public.
        // The signature of an overriding method may differ from the overridden one if a formal parameter in one of the
        // methods has a raw type, while the corresponding parameter in the other has a parameterized type. This
        // accommodates migration of pre-existing code to take advantage of generics.
        // The notion of overriding includes methods that override another from some subclass of their declaring class.
        // This can happen in two ways:
        // A concrete method in a generic superclass can, under certain parameterizations, have the same signature as an
        // abstract method in that class. In this case, the concrete method is inherited and the abstract method is not
        // (as described above). The inherited method should then be considered to override its abstract peer from C.
        // (This scenario is complicated by package access: if C is in a different package, then mA would not have been
        // inherited anyway, and should not be considered overridden.)
        // A method inherited from a class can override a superinterface method. (Happily, package access is not a
        // concern here.)

        if (TypeUtils.isConstructor(overrider)) {
            return false;
        }

        if (overrider.getModifiers().contains(Modifier.STATIC)) {
            // static methods don't override
            return false;
        }

        if (overrider == overridden) {
            return true;
        }

        if (!overrider.getSimpleName().contentEquals(overridden.getSimpleName())) {
            return false;
        }

        if (!TypeUtils.isMemberOf(overridden, type)) {
            return false;
        }

        return TypeUtils.isSubSignature((ExecutableType) overrider.asType(),
                (ExecutableType) TypeUtils.asMemberOf(type.asType(), overridden), universe);
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
        if (!type.getKind().isInterface()) {
            return false;
        } else {
            return isFunctionalInterface.apply(type);
        }
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
