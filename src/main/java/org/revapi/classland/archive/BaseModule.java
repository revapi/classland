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

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

/**
 * This is a helper class for locating the base modules of the different JDK versions.
 */
public final class BaseModule {

    private BaseModule() {

    }

    public static Archive forCurrentJvm() throws IOException {
        String javaVersion = System.getProperty("java.specification.version");
        if (javaVersion.startsWith("1.")) {
            return java8();
        } else {
            return java9();
        }
    }

    /**
     * Tries to find the rt.jar of Java 8- under the directory designated by the {@literal java.home} system property.
     * 
     * @throws IOException
     *             on error to open the file
     */
    public static Archive java8() throws IOException {
        return java8(new File(System.getProperty("java.home")));
    }

    /**
     * Tries to find the rt.jar of Java 8- under the provided java home directory.
     * 
     * @throws IOException
     *             on error to open the file
     */
    public static Archive java8(File javaHome) throws IOException {
        File rtJar = new File(new File(javaHome, "lib"), "rt.jar");
        if (!rtJar.exists()) {
            rtJar = new File(new File(new File(javaHome, "jre"), "lib"), "rt.jar");
        }

        if (!rtJar.exists() || !rtJar.canRead() || !rtJar.isFile()) {
            throw new IllegalStateException(
                    "Could not locate rt.jar under java home '" + javaHome.getAbsolutePath() + "'.");
        }

        return new JarFileArchive(new JarFile(rtJar));
    }

    /**
     * Tries to find the java.base module of Java 9+ under the directory designated by the {@literal java.home} system
     * property.
     * 
     * @throws IOException
     *             on error to open the file
     */
    public static Archive java9() throws IOException {
        return java9(new File(System.getProperty("java.home")));
    }

    /**
     * Tries to find the java.base module of Java 9+ under the provided java home directory.
     * 
     * @throws IOException
     *             on error to open the file
     */
    public static Archive java9(File javaHome) throws IOException {
        File jmods = new File(javaHome, "jmods");
        return new JModArchive(new File(jmods, "java.base.jmod").toPath());
    }
}
