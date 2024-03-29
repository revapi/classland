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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.revapi.classland.archive.AbstractClassData;

public class MemoryClassData extends AbstractClassData {
    private final byte[] data;

    public MemoryClassData(String name, InputStream data) throws IOException {
        super(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int cnt;
        while ((cnt = data.read(buffer)) != -1) {
            out.write(buffer, 0, cnt);
        }

        this.data = out.toByteArray();
    }

    @Override
    public InputStream read() {
        return new ByteArrayInputStream(data);
    }
}
