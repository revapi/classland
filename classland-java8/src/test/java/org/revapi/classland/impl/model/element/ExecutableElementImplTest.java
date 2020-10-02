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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.archive.JarFileArchive;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.IntersectionTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.mirror.TypeVariableImpl;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
public class ExecutableElementImplTest {
    @JarSources(root = "/src/", sources = { "pkg/Methods.java" })
    CompiledJar methods;

    Universe universe;

    @BeforeEach
    void setup() throws Exception {
        universe = new Universe(false);
        universe.registerArchive(new JarFileArchive(new JarFile(methods.jarFile())));
    }

    @AfterEach
    void teardown() throws Exception {
        universe.close();
    }

    @Test
    void defaultMethods() throws Exception {
        TypeElementBase DefaultMethods = universe.getTypeByInternalNameFromModule("pkg/Methods$DefaultMethods", null);

        Assertions.assertNotNull(DefaultMethods);

        List<ExecutableElement> methods = ElementFilter.methodsIn(DefaultMethods.getEnclosedElements());

        assertEquals(2, methods.size());
        assertTrue(methods.stream().anyMatch(m -> !m.isDefault()));
        assertTrue(methods.stream().anyMatch(ExecutableElement::isDefault));
    }

    @Test
    void elementKinds() throws Exception {
        TypeElementBase ElementKinds = universe.getTypeByInternalNameFromModule("pkg/Methods$ElementKinds", null);

        Assertions.assertNotNull(ElementKinds);

        List<? extends Element> enclosed = ElementKinds.getEnclosedElements();

        assertEquals(3, enclosed.size());

        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.CONSTRUCTOR).count());
        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.METHOD).count());
        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.STATIC_INIT).count());
    }

    @Test
    void noTypeParameters() {
        TypeElementBase Methods = universe.getTypeByInternalNameFromModule("pkg/Methods$Generics", null);
        assertNotNull(Methods);

        ExecutableElementImpl nonGeneric = findSingleMethodByName(Methods, "nonGeneric");
        assertNotNull(nonGeneric);

        assertEquals(0, nonGeneric.getTypeParameters().size());
    }

    @Test
    void declaredTypeParameters() {
        TypeElementBase Methods = universe.getTypeByInternalNameFromModule("pkg/Methods$Generics", null);
        assertNotNull(Methods);

        ExecutableElementImpl genericByMethodTypeParam = findSingleMethodByName(Methods, "genericByMethodTypeParam");
        assertNotNull(genericByMethodTypeParam);

        assertEquals(1, genericByMethodTypeParam.getTypeParameters().size());
        TypeParameterElementImpl param = genericByMethodTypeParam.getTypeParameters().get(0);
        assertEquals("U", param.getSimpleName().toString());

        assertEquals(1, param.getBounds().size());
        TypeMirrorImpl b = param.getBounds().get(0);
        assertTrue(b instanceof DeclaredTypeImpl);
        assertEquals("java/lang/String", ((TypeElementBase) ((DeclaredTypeImpl) b).asElement()).internalName);
    }

    @Test
    void typeParametersFromEnclosingType() {
        TypeElementBase Methods = universe.getTypeByInternalNameFromModule("pkg/Methods$Generics", null);
        assertNotNull(Methods);

        ExecutableElementImpl genericByTypeTypeParam = findSingleMethodByName(Methods, "genericByTypeTypeParam");
        assertNotNull(genericByTypeTypeParam);

        assertTrue(genericByTypeTypeParam.getTypeParameters().isEmpty());
        assertEquals(1, genericByTypeTypeParam.getParameters().size());
        TypeMirrorImpl p = genericByTypeTypeParam.getParameters().get(0).asType();
        assertTrue(p instanceof TypeVariableImpl);
        TypeMirrorImpl b = ((TypeVariableImpl) p).getUpperBound();
        assertTrue(b instanceof DeclaredTypeImpl);
        assertEquals("java/lang/Object", ((TypeElementBase) ((DeclaredTypeImpl) b).asElement()).internalName);

        TypeParameterElementImpl param = (TypeParameterElementImpl) ((TypeVariableImpl) p).asElement();
        assertEquals("T", param.getSimpleName().toString());

        assertEquals(1, param.getBounds().size());
        b = param.getBounds().get(0);
        assertTrue(b instanceof DeclaredTypeImpl);
        assertEquals("java/lang/Object", ((TypeElementBase) ((DeclaredTypeImpl) b).asElement()).internalName);
    }

    @Test
    void declaredTypeParameterUsingTypeParameterFromEnclosingType() {
        TypeElementBase Methods = universe.getTypeByInternalNameFromModule("pkg/Methods$Generics", null);
        assertNotNull(Methods);

        ExecutableElementImpl methodTypeParamUsesTypeTypeParam = findSingleMethodByName(Methods,
                "methodTypeParamUsesTypeTypeParam");
        assertNotNull(methodTypeParamUsesTypeTypeParam);

        assertEquals(1, methodTypeParamUsesTypeTypeParam.getTypeParameters().size());
        TypeParameterElementImpl param = methodTypeParamUsesTypeTypeParam.getTypeParameters().get(0);
        assertEquals("U", param.getSimpleName().toString());

        assertEquals(1, param.getBounds().size());
        TypeMirrorImpl b = param.getBounds().get(0);
        assertTrue(b instanceof TypeVariableImpl);
        assertEquals("java/lang/Object", ((TypeElementBase) ((DeclaredTypeImpl) ((TypeVariableImpl) b).getUpperBound())
                .asElement()).internalName);
    }

    @Test
    void typeParametersInInnerClass() {
        TypeElementBase Methods$Inner = universe.getTypeByInternalNameFromModule("pkg/Methods$Generics$Inner", null);
        assertNotNull(Methods$Inner);

        ExecutableElementImpl methodGenericFromEnclosingType = findSingleMethodByName(Methods$Inner,
                "methodGenericFromEnclosingType");
        assertNotNull(methodGenericFromEnclosingType);

        assertTrue(methodGenericFromEnclosingType.getTypeParameters().isEmpty());
        assertEquals(2, methodGenericFromEnclosingType.getParameters().size());

        TypeMirrorImpl p = methodGenericFromEnclosingType.getParameters().get(0).asType();
        assertTrue(p instanceof TypeVariableImpl);
        TypeMirrorImpl b = ((TypeVariableImpl) p).getUpperBound();
        assertTrue(b instanceof DeclaredTypeImpl);
        assertEquals("java/lang/Object", ((TypeElementBase) ((DeclaredTypeImpl) b).asElement()).internalName);

        p = methodGenericFromEnclosingType.getParameters().get(1).asType();
        assertTrue(p instanceof TypeVariableImpl);
        b = ((TypeVariableImpl) p).getUpperBound();
        assertTrue(b instanceof IntersectionTypeImpl);
        assertEquals(2, ((IntersectionTypeImpl) b).getBounds().size());
        TypeMirrorImpl firstBound = ((IntersectionTypeImpl) b).getBounds().get(0);
        TypeMirrorImpl secondBound = ((IntersectionTypeImpl) b).getBounds().get(1);

        assertEquals("java/lang/String", ((TypeElementBase) ((DeclaredTypeImpl) firstBound).asElement()).internalName);
        assertEquals("java/lang/Cloneable",
                ((TypeElementBase) ((DeclaredTypeImpl) secondBound).asElement()).internalName);
    }

    @Test
    void emptyThrowsList() {
        TypeElementBase Exceptions = universe.getTypeByInternalNameFromModule("pkg/Methods$Exceptions", null);
        assertNotNull(Exceptions);

        ExecutableElementImpl noThrows = findSingleMethodByName(Exceptions, "noThrows");
        assertNotNull(noThrows);

        assertTrue(noThrows.getThrownTypes().isEmpty());
    }

    @Test
    void throwsChecked() {
        TypeElementBase Exceptions = universe.getTypeByInternalNameFromModule("pkg/Methods$Exceptions", null);
        assertNotNull(Exceptions);

        ExecutableElementImpl throwsChecked = findSingleMethodByName(Exceptions, "throwsChecked");
        assertNotNull(throwsChecked);

        assertEquals(1, throwsChecked.getThrownTypes().size());
        TypeMirrorImpl ex = throwsChecked.getThrownTypes().get(0);
        assertTrue(ex instanceof DeclaredTypeImpl);

        assertEquals("java/lang/Exception", ((TypeElementBase) ((DeclaredTypeImpl) ex).asElement()).internalName);
    }

    @Test
    void throwsUnchecked() {
        TypeElementBase Exceptions = universe.getTypeByInternalNameFromModule("pkg/Methods$Exceptions", null);
        assertNotNull(Exceptions);

        ExecutableElementImpl throwsUnchecked = findSingleMethodByName(Exceptions, "throwsUnchecked");
        assertNotNull(throwsUnchecked);

        assertEquals(1, throwsUnchecked.getThrownTypes().size());
        TypeMirrorImpl ex = throwsUnchecked.getThrownTypes().get(0);
        assertTrue(ex instanceof DeclaredTypeImpl);

        assertEquals("java/lang/RuntimeException", ((TypeElementBase) ((DeclaredTypeImpl) ex).asElement()).internalName);
    }

    @Test
    void throwsTypeParam() {
        TypeElementBase Exceptions = universe.getTypeByInternalNameFromModule("pkg/Methods$Exceptions", null);
        assertNotNull(Exceptions);

        ExecutableElementImpl throwsTypeParam = findSingleMethodByName(Exceptions, "throwsTypeParam");
        assertNotNull(throwsTypeParam);

        assertEquals(1, throwsTypeParam.getThrownTypes().size());
        TypeMirrorImpl ex = throwsTypeParam.getThrownTypes().get(0);
        assertTrue(ex instanceof TypeVariableImpl);

        assertEquals("java/lang/Throwable", ((TypeElementBase) ((DeclaredTypeImpl) ((TypeVariableImpl) ex).getUpperBound()).asElement()).internalName);
    }

    @Test
    void throwsMany() {
        TypeElementBase Exceptions = universe.getTypeByInternalNameFromModule("pkg/Methods$Exceptions", null);
        assertNotNull(Exceptions);

        ExecutableElementImpl throwsMany = findSingleMethodByName(Exceptions, "throwsMany");
        assertNotNull(throwsMany);

        assertEquals(3, throwsMany.getThrownTypes().size());

        TypeMirrorImpl ex = throwsMany.getThrownTypes().get(0);
        assertTrue(ex instanceof DeclaredTypeImpl);
        assertEquals("java/lang/Exception", ((TypeElementBase) ((DeclaredTypeImpl) ex).asElement()).internalName);

        ex = throwsMany.getThrownTypes().get(1);
        assertTrue(ex instanceof TypeVariableImpl);
        assertEquals("java/lang/RuntimeException", ((TypeElementBase) ((DeclaredTypeImpl) ((TypeVariableImpl) ex).getUpperBound()).asElement()).internalName);

        ex = throwsMany.getThrownTypes().get(2);
        assertTrue(ex instanceof DeclaredTypeImpl);
        assertEquals("java/lang/Throwable", ((TypeElementBase) ((DeclaredTypeImpl) ex).asElement()).internalName);
    }

    ExecutableElementImpl findSingleMethodByName(ElementImpl parent, String methodName) {
        List<? extends ExecutableElement> ms = ElementFilter.methodsIn(parent.getEnclosedElements()).stream()
                .filter(m -> methodName.contentEquals(m.getSimpleName())).collect(Collectors.toList());

        if (ms.isEmpty()) {
            fail("Failed to find method '" + methodName + "' in " + parent.toString());
        }

        if (ms.size() > 1) {
            fail("Found multiple methods called '" + methodName + "' in " + parent.toString());
        }

        return (ExecutableElementImpl) ms.get(0);
    }
}
