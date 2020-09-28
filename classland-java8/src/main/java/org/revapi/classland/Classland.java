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
package org.revapi.classland;

import java.util.function.Supplier;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.impl.ElementsImpl;
import org.revapi.classland.impl.TypesImpl;
import org.revapi.classland.impl.Universe;

public final class Classland implements AutoCloseable {
    private final Universe universe;

    public Classland() {
        this(currentJvmSupportsModules());
    }

    public Classland(boolean analyzeModules) {
        this.universe = new Universe(analyzeModules);
    }

    private static boolean currentJvmSupportsModules() {
        String javaVersion = System.getProperty("java.specification.version");
        return !javaVersion.startsWith("1.");
    }

    /**
     * Registers the archive with Classland. The archive is then managed by Classland and closed upon closing this
     * instance. This is why this method doesn't accept a mere instance of an archive but rather a "factory" for it, so
     * that it is made more clear that the caller is not supposed to manage the archive instance after it has been
     * passed here.
     *
     * @param supplier
     *            the supplier of an archive
     */
    public void registerArchive(Supplier<Archive> supplier) {
        universe.registerArchive(supplier.get());
    }

    public Elements getElements() {
        return new ElementsImpl(universe);
    }

    public Types getTypes() {
        return new TypesImpl(universe);
    }

    @Override
    public void close() throws Exception {
        universe.close();
    }
}
