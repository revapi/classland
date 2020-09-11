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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryModuleSource implements ModuleSource {
    private final File rootDir;

    public DirectoryModuleSource(File rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public Iterator<ClassData> iterator() {
        try (Stream<Path> str = Files.find(rootDir.toPath(), Integer.MAX_VALUE,
                (p, attrs) -> p.getName(p.getNameCount()).toString().endsWith(".class"),
                FileVisitOption.FOLLOW_LINKS)) {
            List<ClassData> paths = str.map(FileClassData::new).collect(Collectors.toList());
            return paths.iterator();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to traverse the directory " + rootDir.getAbsolutePath()
                    + " while looking for class files.");
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DirectoryModuleSource classData = (DirectoryModuleSource) o;
        return rootDir.equals(classData.rootDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootDir);
    }

    @Override
    public String toString() {
        return "DirectoryModuleSource{" + "rootDir=" + rootDir + '}';
    }
}
