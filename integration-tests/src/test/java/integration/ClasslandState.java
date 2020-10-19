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

import javax.lang.model.util.Elements;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.revapi.classland.Classland;
import org.revapi.classland.archive.jar.JarFileArchive;

@State(Scope.Benchmark)
public class ClasslandState extends GuavaJar {
    Elements elements;
    private Classland classland;

    @Setup
    public void setup() throws Exception {
        classland = Classland.builder().withModules(true).withStandardRuntime()
                .addArchive(new JarFileArchive(new JarFile(compiled().jarFile()))).build();

        elements = classland.getElements();
    }

    @TearDown
    public void teardown() throws Exception {
        classland.close();
        cleanup();
    }
}
