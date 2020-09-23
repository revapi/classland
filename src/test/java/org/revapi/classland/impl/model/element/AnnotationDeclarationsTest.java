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
package org.revapi.classland.impl.model.element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.jar.JarFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.module.JarFileModuleSource;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
class AnnotationDeclarationsTest {

    @JarSources(root = "/src/", sources = "pkg/Annotations.java")
    CompiledJar annotations;

    Universe u;
    TypeElementImpl VisibleAnno;
    TypeElementImpl InvisibleAnno;

    @BeforeEach
    void setupTypeUniverse() throws Exception {
        u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(annotations.jarFile())));
        VisibleAnno = (TypeElementImpl) u.getTypeByInternalName("pkg/Annotations$VisibleAnno");
        InvisibleAnno = (TypeElementImpl) u.getTypeByInternalName("pkg/Annotations$InvisibleAnno");
    }

    @Test
    void onType() throws Exception {
        TypeElementImpl AnnotatedClass = (TypeElementImpl) u.getTypeByInternalName("pkg/Annotations$AnnotatedClass");

        List<AnnotationMirrorImpl> annos = AnnotatedClass.getAnnotationMirrors();
        assertEquals(2, annos.size());
        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());
    }

    @Test
    void onTypeParameter() throws Exception {
        TypeElementImpl AnnotatedTypeParameter = (TypeElementImpl) u
                .getTypeByInternalName("pkg/Annotations$AnnotatedTypeParameter");

        List<TypeParameterElementImpl> typeParams = AnnotatedTypeParameter.getTypeParameters();
        assertEquals(2, typeParams.size());
        List<AnnotationMirrorImpl> annos = typeParams.get(0).getAnnotationMirrors();
        assertEquals(2, annos.size());
        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());
        assertSame(InvisibleAnno, annos.get(1).getAnnotationType().asElement());
        annos = typeParams.get(1).getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(InvisibleAnno, annos.get(0).getAnnotationType().asElement());
    }

    @Test
    void onMethod() throws Exception {
        // TODO implement
    }

    @Test
    void onMethodParameter() throws Exception {
        TypeElementImpl AnnotatedMethodParameter = (TypeElementImpl) u
                .getTypeByInternalName("pkg/Annotations$AnnotatedMethodParameter");
        ExecutableElementImpl method = AnnotatedMethodParameter.getMethod("method", "(ID)V");

        assertNotNull(method);

        assertEquals(2, method.getParameters().size());
        VariableElementImpl p1 = method.getParameters().get(0);
        VariableElementImpl p2 = method.getParameters().get(1);

        List<AnnotationMirrorImpl> annos = p1.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());

        annos = p2.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(InvisibleAnno, annos.get(0).getAnnotationType().asElement());
    }

    @Test
    @Disabled("until implemented")
    void onMethodParameterTypeVariable() throws Exception {
        TypeElementImpl AnnotatedMethodParameterTypeVariable = (TypeElementImpl) u
                .getTypeByInternalName("pkg/Annotations$AnnotatedMethodParameterTypeVariable");
        ExecutableElementImpl method = AnnotatedMethodParameterTypeVariable.getMethod("method", "(Ljava/util/Set;)V");

        assertNotNull(method);

        assertEquals(1, method.getParameters().size());
        VariableElementImpl param = method.getParameters().get(0);

        List<AnnotationMirrorImpl> annos = param.getAnnotationMirrors();
        assertEquals(1, annos.size());
        // TODO implement
    }
}
