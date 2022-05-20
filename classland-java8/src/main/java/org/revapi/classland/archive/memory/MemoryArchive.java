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
package org.revapi.classland.archive.memory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;
import org.revapi.classland.impl.util.Nullable;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MemoryArchive implements Archive {
    private final Map<String, ClassData> classes;
    private final Optional<ClassData> moduleInfo;
    private final Optional<Manifest> manifest;

    public MemoryArchive(InputStream jarFile) throws IOException {
        this.classes = new LinkedHashMap<>();
        ClassData moduleInfo = null;
        Manifest manifest = null;
        try (ZipInputStream zis = new ZipInputStream(jarFile)) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if ("module-info.class".equals(ze.getName())) {
                    moduleInfo = new MemoryClassData(ze.getName(), zis);
                } else if (ze.getName().endsWith(".class")) {
                    classes.put(ze.getName(), new MemoryClassData(ze.getName(), zis));
                } else if ("META-INF/MANIFEST.MF".equals(ze.getName())) {
                    manifest = new Manifest(zis);
                }
            }
        }

        this.moduleInfo = Optional.ofNullable(moduleInfo);
        this.manifest = Optional.ofNullable(manifest);
    }

    @Override
    public Optional<Manifest> getManifest() throws IOException {
        return manifest;
    }

    @Override
    public Optional<ClassData> getModuleInfo() throws IOException {
        return moduleInfo;
    }

    @Override
    public void close() throws Exception {
        classes.clear();
    }

    @Override
    public Iterator<ClassData> iterator() {
        return classes.values().iterator();
    }
}
