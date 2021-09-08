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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.revapi.classland.archive.BaseModule;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.CompilerManager;

public class TypesImplTest {
    private static final Arguments classLandElementsAndTypes;
    private static final Arguments javacElementsAndTypes;

    static {
        try {
            Universe u = new Universe(false);
            u.registerArchive(BaseModule.forCurrentJvm());

            classLandElementsAndTypes = Arguments.of(new ElementsImpl(u), new TypesImpl(u));

            CompiledJar jar = new CompilerManager().createJar()
                    // we need any kind of source file
                    .classPathSources("/src/impl/types/orig/", "types/A.java").build();
            CompiledJar.Environment env = jar.analyze();

            javacElementsAndTypes = Arguments.of(env.elements(), env.types());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Stream<Arguments> elementsAndTypes() throws IOException {
        return Stream.of(javacElementsAndTypes, classLandElementsAndTypes);
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testAsElement(Elements els, Types ts) {
        TypeMirror javaLangObject = els.getTypeElement("java.lang.Object").asType();

        Element converted = ts.asElement(javaLangObject);
        assertEquals("Object", converted.getSimpleName().toString());

        TypeElement Comparable = els.getTypeElement("java.lang.Comparable");
        TypeMirror typeVar = Comparable.getTypeParameters().get(0).asType();
        assertSame(TypeKind.TYPEVAR, typeVar.getKind());

        converted = ts.asElement(typeVar);
        assertSame(ElementKind.TYPE_PARAMETER, converted.getKind());
        assertEquals("T", converted.getSimpleName().toString());
        assertSame(Comparable, converted.getEnclosingElement());

        // TODO implement - intersection types
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testIsSameType_sameType(Elements els, Types ts) {
        TypeElement javaLangObject = els.getTypeElement("java.lang.Object");

        TypeMirror t1 = javaLangObject.asType();
        TypeMirror t2 = javaLangObject.asType();

        assertTrue(ts.isSameType(t1, t2));
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testIsSameType_wildcards(Elements els, Types ts) throws Exception {
        TypeElement Comparator = els.getTypeElement("java.util.Comparator");
        TypeElement Collections = els.getTypeElement("java.util.Collections");

        ExecutableElement thenComparing = ElementFilter.methodsIn(Comparator.getEnclosedElements()).stream()
                .filter(m -> m.getSimpleName().contentEquals("thenComparing"))
                .filter(m -> m.getParameters().size() == 1)
                .filter(m -> m.getParameters().get(0).asType().toString().equals("java.util.Comparator<? super T>"))
                .findFirst().orElseThrow(NoSuchElementException::new);
        ExecutableElement sort = ElementFilter.methodsIn(Collections.getEnclosedElements()).stream()
                .filter(m -> m.getSimpleName().contentEquals("sort")).filter(m -> m.getParameters().size() == 2)
                .findFirst().orElseThrow(NoSuchElementException::new);

        DeclaredType comparingPar = (DeclaredType) thenComparing.getParameters().get(0).asType();
        TypeMirror wildcard1 = comparingPar.getTypeArguments().get(0);

        DeclaredType sortPar = (DeclaredType) sort.getParameters().get(1).asType();
        TypeMirror wildcard2 = sortPar.getTypeArguments().get(0);

        // wildcards are never the same
        assertFalse(ts.isSameType(wildcard1, wildcard2));
        assertFalse(ts.isSameType(wildcard1, wildcard1));
        assertFalse(ts.isSameType(wildcard1, comparingPar));
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testIsSameType_primitives(Elements els, Types ts) throws Exception {
        TypeMirror object = els.getTypeElement("java.lang.Object").asType();
        for (TypeKind tk1 : TypeKind.values()) {
            if (!tk1.isPrimitive()) {
                continue;
            }

            PrimitiveType t1 = ts.getPrimitiveType(tk1);
            for (TypeKind tk2 : TypeKind.values()) {
                if (!tk2.isPrimitive()) {
                    continue;
                }

                PrimitiveType t2 = ts.getPrimitiveType(tk2);

                assertEquals(tk1 == tk2, ts.isSameType(t1, t2));
            }
            assertFalse(ts.isSameType(t1, object));
        }
    }

    @Test
    void testIsSameType_typeVars() throws Exception {
        // TODO implement
    }

    @Test
    void testIsSameType_declaredType() throws Exception {
        // TODO implement
    }

    @Test
    void testIsSameType_arrays() throws Exception {
        // TODO implement
    }

    @Test
    void testIsSameType_methods() throws Exception {
        // TODO implement
    }

    @Test
    void testIsSameType_packages() throws Exception {
        // TODO implement
    }

    @Test
    void testIsSameType_errorType() throws Exception {
        // TODO implement
    }

    @Test
    void testIsSubtype() {
        // TODO implement
    }

    @Test
    void testIsAssignable() {
        // TODO implement
    }

    @Test
    void testContains() {
        // TODO implement
    }

    @Test
    void testIsSubsignature() {
        // TODO implement
    }

    @Test
    void testDirectSupertypes() {
        // TODO implement
    }

    @Test
    void testErasure() {
        // TODO implement
    }

    @Test
    void testBoxedClass() {
        // TODO implement
    }

    @Test
    void testUnboxedType() {
        // TODO implement
    }

    @Test
    void testCapture() {
        // TODO implement
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testGetPrimitiveType(Elements els, Types ts) {
        for (TypeKind tk : TypeKind.values()) {
            if (!tk.isPrimitive()) {
                continue;
            }

            PrimitiveType t = ts.getPrimitiveType(tk);
            assertNotNull(t);
            assertSame(tk, t.getKind());
        }
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testGetNullType(Elements els, Types ts) {
        assertNotNull(ts.getNullType());
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testGetNoType(Elements els, Types ts) {
        for (TypeKind tk : TypeKind.values()) {
            if (tk == TypeKind.VOID || tk == TypeKind.NONE) {
                NoType t = ts.getNoType(tk);
                assertSame(tk, t.getKind());
            } else {
                Assertions.assertThrows(IllegalArgumentException.class, () -> ts.getNoType(tk));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testGetArrayType(Elements els, Types ts) {
        ArrayType at = ts.getArrayType(ts.getPrimitiveType(TypeKind.INT));
        assertEquals(ts.getPrimitiveType(TypeKind.INT), at.getComponentType());

        // TODO implement
    }

    @Test
    void testGetWildcardType() {
        // TODO implement
    }

    @Test
    void testGetDeclaredType() {
        // TODO implement
    }

    @Test
    void testAsMemberOf() {
        // TODO implement
    }
}
