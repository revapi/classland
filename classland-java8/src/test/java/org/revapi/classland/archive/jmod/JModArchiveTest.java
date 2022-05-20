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
package org.revapi.classland.archive.jmod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;

import javax.lang.model.type.TypeKind;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.revapi.classland.impl.TypePool;
import org.revapi.classland.impl.model.element.TypeElementBase;

public class JModArchiveTest {

    @ParameterizedTest
    @ValueSource(strings = { "java9.mod", "java10.mod", "java11.mod", "java12.mod", "java14.mod" })
    void loadTest(String jmodFile) throws Exception {
        Path jmod = new File(getClass().getClassLoader().getResource(jmodFile).getPath()).toPath();
        TypePool universe = new TypePool(false);
        universe.registerArchive(new JModArchive(jmod));

        TypeElementBase obj = universe.getLookup()
                .getTypeByInternalNameFromModule("jdk/internal/editor/external/ExternalEditor", null);
        assertEquals(TypeKind.DECLARED, obj.asType().getKind());
        obj = universe.getLookup().getTypeByInternalNameFromModule("not/there/like", null);
        assertEquals(TypeKind.ERROR, obj.asType().getKind());
    }
}
