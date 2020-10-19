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
package org.revapi.classland.archive;

import java.io.IOException;
import java.util.Optional;

/**
 * A module resolver is able to find modules and their archives by name.
 */
public interface ModuleResolver {

    /**
     * Tries to find a module.
     *
     * @param moduleName
     *            the name of the module
     * 
     * @return a non-empty optional if the module is found, empty optional if the module could not be found
     * 
     * @throws IOException
     *             on error while looking for the module
     */
    Optional<Archive> getModuleArchive(String moduleName) throws IOException;
}
