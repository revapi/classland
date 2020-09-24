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
package org.revapi.classland.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.ClassData;
import org.revapi.classland.impl.util.Nullable;

public class ArchiveContents {
    private static final int PACKAGE_CLASS_NAME_LENGTH = "package-info.class".length();
    private final Archive source;
    private volatile boolean scanned = false;
    private final Map<String, @Nullable ClassData> packages = new HashMap<>();
    private final Set<ClassData> classes = new HashSet<>();
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<ClassData> module;

    public ArchiveContents(Archive source) {
        this.source = source;
    }

    public Map<String, @Nullable ClassData> getPackages() {
        scan();
        return packages;
    }

    public Optional<ClassData> getModule() {
        scan();
        return module;
    }

    public Set<ClassData> getTypes() {
        scan();
        return classes;
    }

    private void scan() {
        if (scanned) {
            return;
        }

        synchronized (classes) {
            if (scanned) {
                return;
            }

            ClassData foundModule = null;
            for (ClassData cd : source) {
                String name = cd.getName();
                if (name.equals("module-info")) {
                    foundModule = cd;
                } else if (name.equals("package-info")) {
                    String pkgName = name.substring(0, name.length() - PACKAGE_CLASS_NAME_LENGTH).replace('/', '.');
                    packages.put(pkgName, cd);
                } else {
                    int lastSlash = name.lastIndexOf('/');
                    String pkgName = lastSlash >= 0 ? name.substring(0, name.lastIndexOf('/')).replace('/', '.') : "";
                    if (!packages.containsKey(pkgName)) {
                        packages.put(pkgName, null);
                    }
                    classes.add(cd);
                }
            }

            module = Optional.ofNullable(foundModule);

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
