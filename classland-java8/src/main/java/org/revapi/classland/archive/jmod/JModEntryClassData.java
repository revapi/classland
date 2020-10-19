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
package org.revapi.classland.archive.jmod;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.revapi.classland.archive.AbstractClassData;

public class JModEntryClassData extends AbstractClassData {
    private final ZipFile file;
    private final ZipEntry entry;

    public JModEntryClassData(ZipFile file, ZipEntry entry) {
        super(entry.getName().substring("classes/".length()));
        this.file = file;
        this.entry = entry;
    }

    @Override
    public InputStream read() throws IOException {
        return file.getInputStream(entry);
    }

    @Override
    public String toString() {
        return "JmodEntryClassData{" + "file=" + file + ", entry=" + entry + '}';
    }
}
