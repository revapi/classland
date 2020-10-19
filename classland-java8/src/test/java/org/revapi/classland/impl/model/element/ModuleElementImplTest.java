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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.util.jar.JarFile;

import javax.lang.model.element.ElementKind;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.classland.impl.Universe;

@TestInstance(PER_CLASS)
class ModuleElementImplTest {

    private Universe universe;
    private ModuleElementImpl module;

    @BeforeAll
    void setup() throws Exception {
        JarFile jar = new JarFile(getClass().getClassLoader().getResource("asm-8.0.1.jar").getPath());
        universe = new Universe(true);
        universe.registerArchive(new JarFileArchive(jar));
        assertEquals(2, universe.getModules().size());
        module = universe.getModules().stream().filter(m -> m.getQualifiedName().contentEquals("org.objectweb.asm"))
                .findFirst().get();
    }

    @AfterAll
    void teardown() throws Exception {
        universe.close();
    }

    @Test
    void testCanReadBasicInfo() throws Exception {
        assertTrue("org.objectweb.asm".contentEquals(module.getQualifiedName()));
        assertEquals(ElementKind.OTHER, module.getKind());
    }
}
