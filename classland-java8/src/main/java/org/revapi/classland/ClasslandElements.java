/*
 * Copyright 2020-2021 Lukas Krejci
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

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import org.revapi.classland.impl.util.Nullable;

/**
 * An extension of the {@link Elements} interface providing other utility methods
 */
public interface ClasslandElements extends Elements {
    /**
     * Similar to {@link #getTypeElement(CharSequence)} but using the binary name of the class, not its fully qualified
     * name.
     *
     * @param binaryName
     *            the binary name of the class to look up.
     * 
     * @return the type with given binary name or null if none could be found
     */
    @Nullable
    TypeElement getTypeElementByBinaryName(String binaryName);

    /**
     * Similar to {@link #getTypeElementByBinaryName)} also requiring the name of the module to look up in.
     *
     * Note that this differs from the methods in {@link Elements} for module-aware look up, in that it uses a module
     * name instead of module element instance because we need to keep this interface Java 8 compatible.
     *
     * @param moduleName
     *            the name of the module that should contain the type
     * @param binaryName
     *            the binary name of the class to look up.
     * 
     * @return the type with given binary name in given module or null if none could be found
     */
    @Nullable
    TypeElement getTypeElementByBinaryName(String moduleName, String binaryName);
}
