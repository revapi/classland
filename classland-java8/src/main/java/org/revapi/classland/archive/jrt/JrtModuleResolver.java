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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ModuleResolver;

/**
 * Resolves modules using the JRT file system available since Java 9.
 */
public class JrtModuleResolver implements ModuleResolver {
    private final FileSystem fileSystem;

    public JrtModuleResolver() {
        this(System.getProperty("java.home"));
    }

    public JrtModuleResolver(String javaHomePath) {
        HashMap<String, String> env = new HashMap<>();
        env.put("java.home", javaHomePath);
        try {
            fileSystem = FileSystems.newFileSystem(URI.create("jrt:/"), env);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to construct the JRT file system.", e);
        }
    }

    @Override
    public Optional<Archive> getModuleArchive(String moduleName) {
        Path path = fileSystem.getPath("modules", moduleName);
        if (Files.exists(path)) {
            return Optional.of(new JrtArchive(path));
        } else {
            return Optional.empty();
        }
    }
}
