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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;

/**
 * Reads classes from the jmod files (i.e. the files used for the JDK modules).
 */
public class JModArchive implements Archive {
    private final ZipFile jmodFile;

    public JModArchive(Path jmodFile) throws IOException {
        this.jmodFile = new ZipFile(jmodFile.toFile());
    }

    @Override
    public void close() throws Exception {
        jmodFile.close();
    }

    @Override
    public Iterator<ClassData> iterator() {
        return jmodFile.stream().filter(e -> e.getName().startsWith("classes"))
                .filter(e -> !e.getName().equals("classes/module-info.class"))
                .filter(e -> e.getName().endsWith(".class")).map(e -> (ClassData) new JModEntryClassData(jmodFile, e))
                .iterator();
    }

    @Override
    public Optional<Manifest> getManifest() throws IOException {
        ZipEntry entry = jmodFile.getEntry("classes/META-INF/MANIFEST.MF");
        if (entry == null) {
            return Optional.empty();
        } else {
            try (InputStream in = jmodFile.getInputStream(entry)) {
                return Optional.of(new Manifest(in));
            }
        }
    }

    @Override
    public Optional<ClassData> getModuleInfo() {
        ZipEntry entry = jmodFile.getEntry("classes/module-info.class");
        if (entry == null) {
            return Optional.empty();
        } else {
            return Optional.of(new JModEntryClassData(jmodFile, entry));
        }
    }
}
