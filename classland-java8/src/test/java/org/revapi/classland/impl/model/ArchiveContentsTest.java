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
package org.revapi.classland.impl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.classland.impl.ArchiveContents;

class ArchiveContentsTest {

    @Test
    void testReadingJarFile() throws Exception {
        JarFile jar = new JarFile(getClass().getClassLoader().getResource("asm-8.0.1.jar").getPath());

        try (Archive source = new JarFileArchive(jar)) {
            ArchiveContents contents = new ArchiveContents(source);

            assertTrue(contents.getModule().isPresent());
            ClassData module = contents.getModule().get();
            ClassReader rdr = new ClassReader(module.read());
            ClassNode node = new ClassNode();
            rdr.accept(node, ClassReader.SKIP_CODE & ClassReader.SKIP_FRAMES & ClassReader.SKIP_DEBUG);

            assertNotNull(node.module);
            assertEquals("org.objectweb.asm", node.module.name);

            assertEquals(2, contents.getPackages().size());
            assertTrue(contents.getPackages().containsKey("org.objectweb.asm"));
            assertTrue(contents.getPackages().containsKey("org.objectweb.asm.signature"));

            assertEquals(37, contents.getTypes().size());
            assertTrue(
                    contents.getTypes().stream().anyMatch(cd -> "org/objectweb/asm/ClassReader".equals(cd.getName())));
        }
    }
}
