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

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

public class FindTypeBenchmark {
    @Benchmark
    public void javac(JavacState jarFile, Blackhole hole) {
        find(jarFile.elements, hole);
    }

    @Benchmark
    public void classland(ClasslandState jarFile, Blackhole hole) throws Exception {
        find(jarFile.elements, hole);
    }

    public void find(Elements elements, Blackhole hole) {
        TypeElement el = elements.getTypeElement("com.google.common.annotations.Beta");

        check(el.getQualifiedName().contentEquals("com.google.common.annotations.Beta"));
        check(el.asType().getKind() != TypeKind.ERROR);

        hole.consume(el);
    }

    private static void check(boolean value) {
        if (!value) {
            throw new IllegalStateException();
        }
    }
}
