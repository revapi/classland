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

import java.io.File;
import java.nio.file.Path;

import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.CompilerManager;

class GuavaJar {

    private final CompilerManager manager;
    private final File guavaJar;

    public GuavaJar() {
        manager = new CompilerManager();

        File mavenHome = new File(System.getProperty("user.home"), ".m2");

        Path guavaJarPath = mavenHome.toPath().resolve("repository").resolve("com").resolve("google").resolve("guava")
                .resolve("guava").resolve("29.0-jre").resolve("guava-29.0-jre.jar");

        guavaJar = guavaJarPath.toFile();
    }

    CompiledJar compiled() {
        return manager.jarFrom(guavaJar);
    }

    public void cleanup() {
        manager.cleanUp();
    }
}
