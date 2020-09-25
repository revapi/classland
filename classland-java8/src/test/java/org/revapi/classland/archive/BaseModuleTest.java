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
package org.revapi.classland.archive;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseModuleTest {

    @BeforeAll
    void copyFiles() {

    }

    @AfterAll
    void deleteFiles() {

    }

    @Test
    void findsRtJar() throws IOException {
        File javaHome = new File(getClass().getClassLoader().getResource("fake-java-home").getPath());
        BaseModule.java8(javaHome);
    }

    @Test
    void findsJavaBaseModule() throws IOException {
        File javaHome = new File(getClass().getClassLoader().getResource("fake-java-home").getPath());
        BaseModule.java9(javaHome);
    }
}
