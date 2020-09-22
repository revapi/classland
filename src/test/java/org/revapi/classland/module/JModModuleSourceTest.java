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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;

import javax.lang.model.type.TypeKind;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.element.TypeElementBase;

public class JModModuleSourceTest {

    @ParameterizedTest
    @ValueSource(strings = { "java9.mod", "java10.mod", "java11.mod", "java12.mod", "java14.mod" })
    void loadTest(String jmodFile) throws Exception {
        Path jmod = new File(getClass().getClassLoader().getResource(jmodFile).getPath()).toPath();
        Universe universe = new Universe();
        universe.registerModule(new JModModuleSource(jmod));

        TypeElementBase obj = universe.getTypeByInternalName("jdk/internal/editor/external/ExternalEditor");
        assertEquals(TypeKind.DECLARED, obj.asType().getKind());
        obj = universe.getTypeByInternalName("not/there/like");
        assertEquals(TypeKind.ERROR, obj.asType().getKind());
    }
}
