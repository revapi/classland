/*
 * Copyright 2020-2022 Lukas Krejci
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
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.revapi.classland.archive.BaseModule;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.CompilerManager;

public class TypesImplTest {
    private static final Arguments classLandElementsAndTypes;
    private static final Arguments javacElementsAndTypes;

    static {
        try {
            TypePool u = new TypePool(false);
            u.registerArchive(BaseModule.forCurrentJvm());

            // load the types eagerly to have a fair speed comparison with javac. Both javac and classland then read
            // the contents of the classfiles lazily so let's leave that part out...
            // The comparison is still not completely fair, because javac seems to unzip the classfiles eagerly while
            // classland does so lazily...
            u.getModules().forEach(m -> m.computePackages().get().values().forEach(p -> p.computeTypes().get()));

            classLandElementsAndTypes = Arguments.of(new ElementsImpl(u.getLookup()), new TypesImpl(u.getLookup()));

            CompiledJar jar = new CompilerManager().createJar()
                    // we need any kind of source file
                    .classPathSources("/src/impl/types/orig/", "types/A.java").build();
            CompiledJar.Environment env = jar.analyze();
            javacElementsAndTypes = Arguments.of(env.elements(), env.types());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Stream<Arguments> elementsAndTypes() {
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

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testIsSubtype_typeIsItsOwnSubtype(Elements els, Types types) {
        TypeMirror object = els.getTypeElement("java.lang.Object").asType();
        assertTrue(types.isSubtype(object, object));
    }

    @Test
    @Disabled
    void testIsSubtype_intersectionIsSubtypeIfAllComponentsAre(Elements els, Types types) {
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

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testErasure_declared(Elements els, Types ts) {
        TypeElement Comparable = els.getTypeElement("java.lang.Comparable");
        TypeElement String = els.getTypeElement("java.lang.String");

        DeclaredType ComparableString = ts.getDeclaredType(Comparable, String.asType());

        TypeMirror erased = ts.erasure(ComparableString);
        assertTrue(erased instanceof DeclaredType);
        assertEquals(0, ((DeclaredType) erased).getTypeArguments().size());
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testErasure_array(Elements els, Types ts) {
        TypeElement Comparable = els.getTypeElement("java.lang.Comparable");
        TypeElement String = els.getTypeElement("java.lang.String");

        ArrayType ComparableStringArray = ts.getArrayType(ts.getDeclaredType(Comparable, String.asType()));

        TypeMirror erased = ts.erasure(ComparableStringArray);
        assertTrue(erased instanceof ArrayType);
        assertEquals(0, ((DeclaredType) ((ArrayType) erased).getComponentType()).getTypeArguments().size());
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testErasure_wildcard(Elements els, Types ts) {
        TypeElement String = els.getTypeElement("java.lang.String");

        WildcardType wildcard = ts.getWildcardType(String.asType(), null);

        TypeMirror erased = ts.erasure(wildcard);
        assertTrue(erased instanceof DeclaredType);
        assertEquals("String", ((DeclaredType) erased).asElement().getSimpleName().toString());
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testErasure_executable(Elements els, Types ts) {
        TypeElement Comparable = els.getTypeElement("java.lang.Comparable");
        TypeElement String = els.getTypeElement("java.lang.String");

        DeclaredType ComparableString = ts.getDeclaredType(Comparable, String.asType());

        TypeMirror erased = ts.erasure(ComparableString);
        assertTrue(erased instanceof DeclaredType);
        assertEquals(0, ((DeclaredType) erased).getTypeArguments().size());
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testErasure_typeVariable(Elements els, Types ts) {
        TypeElement Comparable = els.getTypeElement("java.lang.Comparable");
        TypeElement String = els.getTypeElement("java.lang.String");

        DeclaredType ComparableString = ts.getDeclaredType(Comparable, String.asType());

        TypeMirror erased = ts.erasure(ComparableString);
        assertTrue(erased instanceof DeclaredType);
        assertEquals(0, ((DeclaredType) erased).getTypeArguments().size());
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
        assertTrue(ts.isSameType(ts.getPrimitiveType(TypeKind.INT), at.getComponentType()));

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

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testAsMemberOf(Elements els, Types ts) {
        TypeElement Set = els.getTypeElement("java.util.Set");
        ExecutableElement add = ElementFilter.methodsIn(Set.getEnclosedElements()).stream()
                .filter(m -> "add".contentEquals(m.getSimpleName())).findFirst().get();

        TypeMirror StringType = els.getTypeElement("java.lang.String").asType();

        DeclaredType targetType = ts.getDeclaredType(Set, StringType);

        TypeMirror converted = ts.asMemberOf(targetType, add);
        assertSame(TypeKind.EXECUTABLE, converted.getKind());

        ExecutableType addString = (ExecutableType) converted;
        assertEquals(StringType, addString.getParameterTypes().get(0));
    }

    @ParameterizedTest
    @MethodSource("elementsAndTypes")
    void testAsMemberOf_inherited(Elements els, Types ts) {
        TypeElement Enum = els.getTypeElement("java.lang.Enum");
        TypeElement TextStyle = els.getTypeElement("java.time.format.TextStyle");
        ExecutableElement compareTo = ElementFilter.methodsIn(Enum.getEnclosedElements()).stream()
                .filter(m -> "compareTo".contentEquals(m.getSimpleName())).findFirst().get();

        DeclaredType targetType = ts.getDeclaredType(TextStyle);

        TypeMirror converted = ts.asMemberOf(targetType, compareTo);
        assertSame(TypeKind.EXECUTABLE, converted.getKind());

        ExecutableType convertedCompareTo = (ExecutableType) converted;
        assertEquals(targetType, convertedCompareTo.getParameterTypes().get(0));
    }
}
