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

import java.util.Iterator;
import java.util.Objects;
import java.util.jar.JarFile;

public class JarFileModuleSource implements ModuleSource {
    private final JarFile jarFile;

    public JarFileModuleSource(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    @Override
    public Iterator<ClassData> iterator() {
        return jarFile.stream().filter(e -> e.getName().endsWith(".class"))
                .map(e -> (ClassData) new ZipEntryClassData(jarFile, e)).iterator();
    }

    @Override
    public void close() throws Exception {
        jarFile.close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JarFileModuleSource classData = (JarFileModuleSource) o;
        return jarFile.equals(classData.jarFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jarFile);
    }

    @Override
    public String toString() {
        return "JarFileModuleSource{" + "jarFile=" + jarFile + '}';
    }
}
