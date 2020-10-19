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
package integration;

import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

public class TypeNameScanBenchmark {
    @Benchmark
    public void javac(JavacState jarFile, Blackhole hole) {
        readAll(jarFile.elements, hole);
    }

    @Benchmark
    public void classland(ClasslandState jarFile, Blackhole hole) throws Exception {
        readAll(jarFile.elements, hole);
    }

    public void readAll(Elements elements, Blackhole hole) {
        Set<? extends ModuleElement> mods = elements.getAllModuleElements();
        for (ModuleElement module : mods) {
            readPackages(module, hole);
        }
    }

    private void readPackages(ModuleElement element, Blackhole hole) {
        hole.consume(element);
        for (Element e : element.getEnclosedElements()) {
            readTypes((PackageElement) e, hole);
        }
    }

    private void readTypes(PackageElement pkg, Blackhole hole) {
        for (Element t : pkg.getEnclosedElements()) {
            hole.consume(t.getSimpleName());
        }
    }
}
