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

import java.util.List;
import java.util.jar.JarFile;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.archive.JarFileArchive;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
public class ExecutableElementImplTest {
    @JarSources(root = "/src/", sources = { "pkg/Methods.java" })
    CompiledJar methods;

    @Test
    void defaultMethods() throws Exception {
        Universe u = new Universe();
        u.registerArchive(new JarFileArchive(new JarFile(methods.jarFile())));

        TypeElementBase DefaultMethods = u.getTypeByInternalName("pkg/Methods$DefaultMethods");

        Assertions.assertNotNull(DefaultMethods);

        List<ExecutableElement> methods = ElementFilter.methodsIn(DefaultMethods.getEnclosedElements());

        assertEquals(2, methods.size());
        assertTrue(methods.stream().anyMatch(m -> !m.isDefault()));
        assertTrue(methods.stream().anyMatch(ExecutableElement::isDefault));
    }

    @Test
    void elementKinds() throws Exception {
        Universe u = new Universe();
        u.registerArchive(new JarFileArchive(new JarFile(methods.jarFile())));

        TypeElementBase ElementKinds = u.getTypeByInternalName("pkg/Methods$ElementKinds");

        Assertions.assertNotNull(ElementKinds);

        List<? extends Element> enclosed = ElementKinds.getEnclosedElements();

        assertEquals(3, enclosed.size());

        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.CONSTRUCTOR).count());
        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.METHOD).count());
        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.STATIC_INIT).count());
    }
}
