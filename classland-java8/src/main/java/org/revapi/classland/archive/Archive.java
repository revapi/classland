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
package org.revapi.classland.archive;

import java.io.IOException;
import java.util.Optional;
import java.util.jar.Manifest;

/**
 * This is an abstraction of some kind of archive representing java class data. This data represents all kinds of Java
 * elements stored in a class file like types, modules or package info classes.
 */
public interface Archive extends Iterable<ClassData>, AutoCloseable {
    /**
     * Returns the manifest contained in the archive, if any.
     * 
     * @throws IOException
     *             on error reading the manifest file.
     */
    Optional<Manifest> getManifest() throws IOException;

    /**
     * Returns the module-info contained in the archive, if any.
     *
     * @throws IOException
     *             on error reading the module-info
     */
    Optional<ClassData> getModuleInfo() throws IOException;
}
