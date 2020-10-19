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
package org.revapi.classland.impl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.io.Writer;
import java.util.jar.JarFile;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.PrettyPrinting;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@ExtendWith(CompiledJarExtension.class)
class PrettyPrintingTest {
    @JarSources(root = "/src/", sources = "pkg/Enums.java")
    CompiledJar enums;

    @Test
    void printType() throws Exception {
        // TODO implement
    }

    @Test
    void printGenericType() throws Exception {
        // TODO implement
    }

    @Test
    void printEnum() throws Exception {
        Universe u = new Universe(true);
        u.registerArchive(new JarFileArchive(new JarFile(enums.jarFile())));

        TypeElementImpl Enums = (TypeElementImpl) u.getTypeByInternalNameFromModule("pkg/Enums", null);
        ExecutableElement values = ElementFilter.methodsIn(Enums.getEnclosedElements()).stream()
                .filter(m -> "values".contentEquals(m.getSimpleName())).findFirst().get();

        StringWriter all = new StringWriter();
        printAll(u.getUnnamedModule(), all);

        System.out.println(all.toString());

        assertEquals("pkg.Enums", print(Enums));
        assertEquals("pkg.Enums[] pkg.Enums::values()", print(values));
    }

    @Test
    void printField() throws Exception {
        // TODO implement
    }

    @Test
    void printMethod() throws Exception {
        // TODO implement
    }

    @Test
    void printGenericMethod() throws Exception {
        // TODO implement
    }

    @Test
    void printAnnotation() throws Exception {
        // TODO implement
    }

    @Test
    void printAnnotationValue() throws Exception {
        // TODO implement
    }

    private static String print(Element e) {
        StringWriter wrt = new StringWriter();
        PrettyPrinting.print(wrt, e);
        return wrt.toString();
    }

    private static void printAll(Element e, Writer wrt) {
        PrettyPrinting.silentWrite(e, wrt, e.getClass().getSimpleName());
        PrettyPrinting.silentWrite(e, wrt, ": ");
        PrettyPrinting.print(wrt, e);
        PrettyPrinting.silentWrite(e, wrt, "\n");
        for (Element c : e.getEnclosedElements()) {
            printAll(c, wrt);
        }
    }
}
