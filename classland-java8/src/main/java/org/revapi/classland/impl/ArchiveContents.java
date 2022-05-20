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
package org.revapi.classland.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;
import org.revapi.classland.impl.util.Nullable;

public class ArchiveContents {
    private static final int PACKAGE_INFO_NAME_LENGTH = "package-info".length();
    private final Archive source;
    private volatile boolean scanned = false;
    private volatile boolean moduleInfoInitialized = false;
    private final Map<String, @Nullable ClassData> packages = new HashMap<>();
    private final Map<String, Set<ClassData>> classes = new HashMap<>();
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<ClassData> module;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<String> moduleName;

    public ArchiveContents(Archive source) {
        this.source = source;
    }

    public Archive getArchive() {
        return source;
    }

    public Map<String, @Nullable ClassData> getPackages() {
        scan();
        return packages;
    }

    public Optional<ClassData> getModule() {
        initModuleInfo();
        return module;
    }

    public Optional<String> getAutomaticModuleName() {
        initModuleInfo();
        return moduleName;
    }

    /**
     * The keys are package names, values are classes contained in a package.
     */
    public Map<String, Set<ClassData>> getTypes() {
        scan();
        return classes;
    }

    private void initModuleInfo() {
        if (moduleInfoInitialized) {
            return;
        }

        synchronized (classes) {
            if (moduleInfoInitialized) {
                return;
            }

            try {
                this.module = source.getModuleInfo();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to scan the module-info of the archive.", e);
            }

            try {
                this.moduleName = this.module.isPresent() ? Optional.empty()
                        : source.getManifest().map(manifest -> (String) manifest.getMainAttributes()
                                .get(new Attributes.Name("Automatic-Module-Name")));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to scan the manifest of the archive.", e);
            }
            moduleInfoInitialized = true;
        }
    }

    private void scan() {
        if (scanned) {
            return;
        }

        synchronized (classes) {
            if (scanned) {
                return;
            }

            for (ClassData cd : source) {
                String name = cd.getName();
                if (name.endsWith("package-info")) {
                    String pkgName = name.substring(0, name.length() - PACKAGE_INFO_NAME_LENGTH - 1).replace('/', '.');
                    packages.put(pkgName, cd);
                } else if (!name.equals("module-info")) {
                    int lastSlash = name.lastIndexOf('/');
                    String pkgName = lastSlash >= 0 ? name.substring(0, name.lastIndexOf('/')).replace('/', '.') : "";
                    if (!packages.containsKey(pkgName)) {
                        packages.put(pkgName, null);
                    }
                    classes.computeIfAbsent(pkgName, __ -> new HashSet<>()).add(cd);
                }
            }

            scanned = true;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ArchiveContents that = (ArchiveContents) o;
        return source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    @Override
    public String toString() {
        return "ArchiveContents{" + "source=" + source + '}';
    }
}
