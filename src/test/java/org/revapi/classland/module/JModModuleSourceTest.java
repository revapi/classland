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
package org.revapi.classland.module;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.element.TypeElementImpl;

public class JModModuleSourceTest {

    @ParameterizedTest
    @ValueSource(strings = { "java9.jmod", "java10.jmod", "java11.jmod", "java12.jmod", "java14.jmod" })
    void loadTest(String jmodFile) throws Exception {
        Path jmod = new File(getClass().getClassLoader().getResource(jmodFile).getPath()).toPath();
        Universe universe = new Universe();
        universe.registerModule(new JModModuleSource(jmod));

        TypeElementImpl obj = universe.getTypeByInternalName("jdk/internal/editor/external/ExternalEditor")
                .orElse(null);

        assertNotNull(obj);
    }
}
