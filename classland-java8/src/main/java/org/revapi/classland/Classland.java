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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.revapi.classland.archive.Archive;
import org.revapi.classland.archive.BaseModule;
import org.revapi.classland.archive.ModuleResolver;
import org.revapi.classland.archive.jrt.JrtModuleResolver;
import org.revapi.classland.impl.ElementsImpl;
import org.revapi.classland.impl.TypesImpl;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.util.Exceptions;

public final class Classland implements AutoCloseable {
    private final Universe universe;
    private final ElementsImpl elements;
    private final TypesImpl types;

    private Classland(Universe universe) {
        this.universe = universe;
        this.elements = new ElementsImpl(universe);
        this.types = new TypesImpl(universe);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static boolean currentJvmSupportsModules() {
        String javaVersion = System.getProperty("java.specification.version");
        return !javaVersion.startsWith("1.");
    }

    public Elements getElements() {
        return elements;
    }

    public Types getTypes() {
        return types;
    }

    @Override
    public void close() throws Exception {
        universe.close();
    }

    public static final class Builder {
        private boolean analyzeModules = currentJvmSupportsModules();
        private boolean computeModuleClosure;
        private final List<String> modules = new ArrayList<>();
        private final List<Archive> archives = new ArrayList<>();
        private final List<ModuleResolver> moduleResolvers = new ArrayList<>();

        public Builder withModules(boolean value) {
            this.analyzeModules = value;
            return this;
        }

        /**
         * Registers the archive with Classland. The archive is then managed by Classland and closed upon closing the
         * Classland instance.
         */
        public Builder addArchive(Archive archive) {
            this.archives.add(archive);
            return this;
        }

        /**
         * Explicitly adds a module to analyze. This module is searched by name in all the registered module resolvers.
         *
         * @param moduleName
         *            the name of the module to analyze
         */
        public Builder addModule(String moduleName) {
            this.modules.add(moduleName);
            return this;
        }

        /**
         * Registers a module resolver with Classland.
         *
         * @param resolver
         *            the resolver
         */
        public Builder addModuleResolver(ModuleResolver resolver) {
            this.moduleResolvers.add(resolver);
            return this;
        }

        /**
         * Whether or not to compute the full transitive closure of the explicitly added modules.
         */
        public Builder withAllReachableModules(boolean value) {
            this.computeModuleClosure = value;
            return this;
        }

        /**
         * Adds the {@link JrtModuleResolver} and {@code java.base} module. This makes up the standard base runtime of
         * the modern JVMs. This uses the modules of the current JVM.
         */
        public Builder withStandardRuntime() {
            return withStandardRuntime(System.getProperty("java.home"));
        }

        /**
         * Adds the {@link JrtModuleResolver} and {@code java.base} module. This makes up the standard base runtime of
         * the modern JVMs.
         *
         * @param javaHomePath
         *            the path to the java home (can be of different version of the JVM (9+).
         */
        public Builder withStandardRuntime(String javaHomePath) {
            addModuleResolver(new JrtModuleResolver(javaHomePath));
            addModule("java.base");
            return this;
        }

        public Builder withStandardJava8Runtime() {
            return withStandardJava8Runtime(System.getProperty("java.home"));
        }

        public Builder withStandardJava8Runtime(String javaHomePath) {
            try {
                return addArchive(BaseModule.java8(new File(javaHomePath)));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to locate the Java8's rt.jar under " + javaHomePath);
            }
        }

        public Classland build() {
            Universe universe = new Universe(analyzeModules);
            for (Archive a : archives) {
                universe.registerArchive(a);
            }

            for (ModuleResolver r : moduleResolvers) {
                universe.registerModuleResolver(r);
            }

            for (String m : modules) {
                universe.addModule(m);
            }

            if (computeModuleClosure) {
                universe.addModulesClosure();
            }

            return new Classland(universe);
        }
    }
}
