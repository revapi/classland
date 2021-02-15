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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.jar.JarFile;

import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
class ElementsImplTest {

    @JarSources(root = "/src/impl/", sources = {"packages/a/A.java", "packages/b/B.java"})
    private CompiledJar packages;

    @Test
    void testGetPackageElement() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(packages.jarFile())));

        Elements els = new ElementsImpl(u);

        PackageElement pkg = els.getPackageElement("packages.a");
        assertNotNull(pkg);
        assertTrue(pkg.getQualifiedName().contentEquals("packages.a"));

        pkg = els.getPackageElement("packages.b");
        assertNotNull(pkg);
        assertTrue(pkg.getQualifiedName().contentEquals("packages.b"));

        pkg = els.getPackageElement("packages.nonExistent");
        assertNull(pkg);
    }

    @Test
    void testGetTypeElement() {
        // finds in normal circumstances

        // returns null if there are more types with the same fqn.

        // TODO implement
    }

    @Test
    void testGetElementValuesWithDefaults() {
        // TODO implement
    }

    @Test
    void isDeprecated() {
        // TODO implement
    }

    @Test
    void testGetBinaryName() {
        // TODO implement
    }

    @Test
    void testGetPackageOf() {
        // TODO implement
    }

    @Test
    void testGetAllMembers() {
        // TODO implement
    }

    @Test
    void testGetAllAnnotationMirrors() {
        // TODO implement
    }

    @Test
    void testHides() {
        // TODO implement
    }

    @Test
    void testOverrides() {
        // TODO implement
    }

    @Test
    void testGetConstantExpression() {
        // TODO implement
    }

    @Test
    void testPrintElements() {
        // TODO implement
    }

    @Test
    void testGetName() {
        // TODO implement
    }

    @Test
    void testIsFunctionalInterface() {
        // TODO implement
    }
}
