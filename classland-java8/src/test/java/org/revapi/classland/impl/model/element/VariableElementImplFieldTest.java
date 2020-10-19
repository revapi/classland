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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.classland.impl.Universe;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
public class VariableElementImplFieldTest {
    @JarSources(root = "/src/", sources = { "pkg/Fields.java" })
    CompiledJar fields;

    @Test
    void constantValue() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(fields.jarFile())));

        TypeElementBase Fields = u.getTypeByInternalNameFromModule("pkg/Fields", null);
        assertNotNull(Fields);

        List<VariableElement> fields = ElementFilter.fieldsIn(Fields.getEnclosedElements());

        VariableElement staticWithoutValue = fields.stream()
                .filter(f -> "staticWithoutValue".contentEquals(f.getSimpleName())).findFirst().orElse(null);
        VariableElement staticWithValue = fields.stream()
                .filter(f -> "staticWithValue".contentEquals(f.getSimpleName())).findFirst().orElse(null);

        assertNotNull(staticWithoutValue);
        assertNotNull(staticWithValue);

        assertNull(staticWithoutValue.getConstantValue());
        assertEquals(2, staticWithValue.getConstantValue());
    }

    @Test
    void enumConstant() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(fields.jarFile())));

        TypeElementBase Enum = u.getTypeByInternalNameFromModule("pkg/Fields$Enum", null);
        assertNotNull(Enum);

        List<VariableElement> fields = ElementFilter.fieldsIn(Enum.getEnclosedElements());

        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(f -> f.getKind() == ElementKind.ENUM_CONSTANT
                && "VARIANT1".contentEquals(f.getSimpleName())
                && new HashSet<>(asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)).equals(f.getModifiers())));
        assertTrue(fields.stream()
                .anyMatch(f -> f.getKind() == ElementKind.FIELD && "normalField".contentEquals(f.getSimpleName())
                        && new HashSet<>(asList(Modifier.STATIC, Modifier.FINAL)).equals(f.getModifiers())));
    }
}
