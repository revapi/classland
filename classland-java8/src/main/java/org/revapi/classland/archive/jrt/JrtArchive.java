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
package org.revapi.classland.archive.jrt;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;

public class JrtArchive implements Archive {
    private final Path path;

    public JrtArchive(Path path) {
        this.path = path;
    }

    @Override
    public void close() {

    }

    @Override
    public Iterator<ClassData> iterator() {
        try {
            try (Stream<Path> str = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
                // we need to be eager here so that we can correctly close the stream. The iterator interface offers
                // no bulletproof way of doing that.
                return str.filter(p -> Files.isRegularFile(p))
                        .filter(p -> p.getFileName().toString().endsWith(".class"))
                        .map(p -> (ClassData) new JrtClassData(p)).collect(toList()).iterator();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan the JRT archive for class files.", e);
        }
    }

    @Override
    public Optional<Manifest> getManifest() throws IOException {
        // we only need manifests for the automatic module name, which the standard modules in JRT don't have.
        return Optional.empty();
    }
}
