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
import java.io.InputStream;

/**
 * An interface for accessing data of Java class file.
 */
public interface ClassData {
    /**
     * This gives the internal name of the class file as defined by the JVM spec. I.e. the package names are delimited
     * by '/'.
     *
     * @return the internal name of the class file
     */
    String getName();

    /**
     * Returns the stream with the bytecode of the class.
     * 
     * @throws IOException
     *             on error
     */
    InputStream read() throws IOException;
}
