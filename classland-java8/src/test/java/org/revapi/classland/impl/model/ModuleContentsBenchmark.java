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

import java.io.IOException;
import java.util.jar.JarFile;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.classland.impl.ArchiveContents;

public class ModuleContentsBenchmark {

    @State(Scope.Thread)
    public static class StateHolder {
        final Archive source;
        {
            try {
                JarFile testJar = new JarFile(getClass().getClassLoader().getResource("asm-8.0.1.jar").getPath());
                source = new JarFileArchive(testJar);
            } catch (IOException e) {
                throw new IllegalStateException("Could not find the test jar.", e);
            }
        }

        @TearDown
        public void teardown() throws Exception {
            source.close();
        }
    }

    @Benchmark
    public void scanFullJar(StateHolder state, Blackhole hole) throws IOException {
        hole.consume(new ArchiveContents(state.source).getModule());
    }
}
