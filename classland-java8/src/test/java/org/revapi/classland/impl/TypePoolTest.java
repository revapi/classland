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

import java.util.jar.JarFile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.classland.impl.model.element.ModuleElementImpl;

public class TypePoolTest {

    @Test
    void testRegisterArchive() throws Exception {
        JarFile jar = new JarFile(getClass().getClassLoader().getResource("guava-30.1-jre.jar").getPath());

        try (Archive source = new JarFileArchive(jar)) {
            TypePool tp = new TypePool(true);
            tp.registerArchive(source);

            ModuleElementImpl guavaModule = tp.getModule("com.google.common");
            Assertions.assertNotNull(guavaModule);

            // this tests that we don't get any exceptions while gathering all the types in the archive
            guavaModule.computePackages().get().forEach((__, pkg) -> pkg.computeTypes().get());
        }
    }

}
