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

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.module.JarFileModuleSource;
import org.revapi.classland.module.JarFileModuleSourceTest;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
class TypeElementImplTest {
    @JarSources(root = "/src/", sources = { "pkg/names/Dollar$1.java", "pkg/names/Dollars.java" })
    CompiledJar names;

    @JarSources(root = "/src/", sources = { "pkg/Kinds.java" })
    CompiledJar kinds;

    @JarSources(root = "/src/", sources = { "pkg/Modifiers.java" })
    CompiledJar modifiers;

    @Test
    void qualifiedNames() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(names.jarFile())));

        TypeElementImpl top = u.getTypeByInternalName("pkg/names/Dollars").orElse(null);
        TypeElementImpl member = u.getTypeByInternalName("pkg/names/Dollars$Member$1").orElse(null);
        TypeElementImpl anon = u.getTypeByInternalName("pkg/names/Dollars$Member$2").orElse(null);
        TypeElementImpl local = u.getTypeByInternalName("pkg/names/Dollars$Member$2$1LocalInInitializer$InnerInLocal")
                .orElse(null);

        assertNotNull(top);
        assertNotNull(member);
        assertNotNull(anon);
        assertNotNull(local);

        assertTrue("pkg.names.Dollars".contentEquals(top.getQualifiedName()));
        assertTrue("pkg.names.Dollars.Member$1".contentEquals(member.getQualifiedName()));
        assertEquals(0, local.getQualifiedName().length());
        assertEquals(0, anon.getQualifiedName().length());
    }

    @Test
    void simpleNames() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(names.jarFile())));

        TypeElementImpl top = u.getTypeByInternalName("pkg/names/Dollars").orElse(null);
        TypeElementImpl member = u.getTypeByInternalName("pkg/names/Dollars$Member$1").orElse(null);
        TypeElementImpl anon = u.getTypeByInternalName("pkg/names/Dollars$Member$2").orElse(null);
        TypeElementImpl local = u.getTypeByInternalName("pkg/names/Dollars$Member$2$1LocalInInitializer$InnerInLocal")
                .orElse(null);

        assertNotNull(top);
        assertNotNull(member);
        assertNotNull(anon);
        assertNotNull(local);

        assertTrue("Dollars".contentEquals(top.getSimpleName()));
        assertTrue("Member$1".contentEquals(member.getSimpleName()));
        assertTrue("InnerInLocal".contentEquals(local.getSimpleName()));
        assertEquals(0, anon.getSimpleName().length());
    }

    @Test
    void elementKind() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(kinds.jarFile())));

        TypeElementImpl Class = u.getTypeByInternalName("pkg/Kinds$Classes$Class").orElse(null);
        TypeElementImpl Interface = u.getTypeByInternalName("pkg/Kinds$Classes$Interface").orElse(null);
        TypeElementImpl Enum = u.getTypeByInternalName("pkg/Kinds$Classes$Enum").orElse(null);
        TypeElementImpl Annotation = u.getTypeByInternalName("pkg/Kinds$Classes$Annotation").orElse(null);

        assertNotNull(Class);
        assertNotNull(Interface);
        assertNotNull(Enum);
        assertNotNull(Annotation);

        assertEquals(ElementKind.CLASS, Class.getKind());
        assertEquals(ElementKind.INTERFACE, Interface.getKind());
        assertEquals(ElementKind.ENUM, Enum.getKind());
        assertEquals(ElementKind.ANNOTATION_TYPE, Annotation.getKind());
    }

    @Test
    void modifiers() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(modifiers.jarFile())));

        TypeElementImpl Modifiers = u.getTypeByInternalName("pkg/Modifiers").orElse(null);
        TypeElementImpl Public = u.getTypeByInternalName("pkg/Modifiers$Classes$Public").orElse(null);
        TypeElementImpl Protected = u.getTypeByInternalName("pkg/Modifiers$Classes$Protected").orElse(null);
        TypeElementImpl Private = u.getTypeByInternalName("pkg/Modifiers$Classes$Private").orElse(null);
        TypeElementImpl Static = u.getTypeByInternalName("pkg/Modifiers$Classes$Static").orElse(null);
        TypeElementImpl Final = u.getTypeByInternalName("pkg/Modifiers$Classes$Final").orElse(null);
        TypeElementImpl StrictFp = u.getTypeByInternalName("pkg/Modifiers$Classes$StrictFp").orElse(null);
        TypeElementImpl Abstract = u.getTypeByInternalName("pkg/Modifiers$Classes$Abstract").orElse(null);

        assertNotNull(Modifiers);
        assertNotNull(Public);
        assertNotNull(Protected);
        assertNotNull(Private);
        assertNotNull(Static);
        assertNotNull(Final);
        assertNotNull(StrictFp);
        assertNotNull(Abstract);

        assertEquals(asSet(Modifier.PUBLIC, Modifier.ABSTRACT), Modifiers.getModifiers());
        assertEquals(singleton(Modifier.PUBLIC), Public.getModifiers());
        assertEquals(singleton(Modifier.PROTECTED), Protected.getModifiers());
        assertEquals(singleton(Modifier.PRIVATE), Private.getModifiers());
        assertEquals(singleton(Modifier.STATIC), Static.getModifiers());
        assertEquals(singleton(Modifier.FINAL), Final.getModifiers());
        assertEquals(emptySet(), StrictFp.getModifiers()); // not set on class but on all its methods
        assertEquals(singleton(Modifier.ABSTRACT), Abstract.getModifiers());
    }

    @Test
    void innerClasses() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(names.jarFile())));

        TypeElementImpl top = u.getTypeByInternalName("pkg/names/Dollars").orElse(null);

        assertNotNull(top);

        List<TypeElement> innerClasses = ElementFilter.typesIn(top.getEnclosedElements());

        assertEquals(2, innerClasses.size());
        assertTrue(innerClasses.stream().anyMatch(t -> "pkg.names.Dollars.Member$1".contentEquals(t.getQualifiedName())));
        assertTrue(innerClasses.stream().anyMatch(t -> "pkg.names.Dollars.Member".contentEquals(t.getQualifiedName())));
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... vals) {
        return new HashSet<>(asList(vals));
    }
}
