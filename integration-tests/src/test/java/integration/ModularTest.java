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
package integration;

import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;
import org.revapi.classland.Classland;
import org.revapi.classland.archive.JarFileArchive;

class ModularTest {

    @Test
    void test() throws Exception {
        try (Classland classland = new Classland()) {
            JarFile jar = new JarFile(getClass().getClassLoader().getResource("asm-8.0.1.jar").getPath());
            classland.registerArchive(() -> new JarFileArchive(jar));
        }
    }
}
