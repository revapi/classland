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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.jar.JarFile;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.archive.JarFileArchive;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.ArrayTypeImpl;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.PrimitiveTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
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
    TypeElementImpl VisibleTypeAnno;
    TypeElementImpl InvisibleTypeAnno;

    @BeforeEach
    void setupTypeUniverse() throws Exception {
        u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(annotations.jarFile())));
        VisibleAnno = (TypeElementImpl) u.getTypeByInternalNameFromModule("pkg/Annotations$VisibleAnno", null);
        InvisibleAnno = (TypeElementImpl) u.getTypeByInternalNameFromModule("pkg/Annotations$InvisibleAnno", null);
        VisibleTypeAnno = (TypeElementImpl) u.getTypeByInternalNameFromModule("pkg/Annotations$VisibleTypeAnno", null);
        InvisibleTypeAnno = (TypeElementImpl) u.getTypeByInternalNameFromModule("pkg/Annotations$InvisibleTypeAnno",
                null);
    }

    @Test
    void onType() throws Exception {
        TypeElementImpl AnnotatedClass = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedClass", null);

        List<AnnotationMirrorImpl> annos = AnnotatedClass.getAnnotationMirrors();
        assertEquals(2, annos.size());
        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());
        assertSame(InvisibleTypeAnno, annos.get(1).getAnnotationType().asElement());
    }

    @Test
    void onTypeParameter() throws Exception {
        TypeElementImpl AnnotatedTypeParameter = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedTypeParameter", null);

        List<TypeParameterElementImpl> typeParams = AnnotatedTypeParameter.getTypeParameters();
        assertEquals(2, typeParams.size());
        List<AnnotationMirrorImpl> annos = typeParams.get(0).getAnnotationMirrors();
        assertEquals(2, annos.size());
        assertSame(VisibleTypeAnno, annos.get(0).getAnnotationType().asElement());
        assertSame(InvisibleTypeAnno, annos.get(1).getAnnotationType().asElement());
        annos = typeParams.get(1).getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(InvisibleTypeAnno, annos.get(0).getAnnotationType().asElement());
    }

    @Test
    void onMethod() throws Exception {
        TypeElementImpl AnnotatedMethod = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedMethod", null);

        ExecutableElementImpl method = AnnotatedMethod.getMethod("method", "()V");

        assertNotNull(method);

        List<AnnotationMirrorImpl> annos = method.getAnnotationMirrors();
        assertEquals(2, annos.size());
        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());
        assertSame(VisibleTypeAnno, annos.get(1).getAnnotationType().asElement());
    }

    @Test
    void onMethodParameter() throws Exception {
        TypeElementImpl AnnotatedMethodParameter = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedMethodParameter", null);
        ExecutableElementImpl method = AnnotatedMethodParameter.getMethod("method",
                "(IDLjava/lang/String;Ljava/lang/Object;)V");

        assertNotNull(method);

        assertEquals(4, method.getParameters().size());
        VariableElementImpl p1 = method.getParameters().get(0);
        VariableElementImpl p2 = method.getParameters().get(1);
        VariableElementImpl p3 = method.getParameters().get(2);
        VariableElementImpl p4 = method.getParameters().get(3);

        List<AnnotationMirrorImpl> annos = p1.getAnnotationMirrors();
        assertEquals(2, annos.size());
        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());
        assertSame(VisibleTypeAnno, annos.get(1).getAnnotationType().asElement());

        annos = p2.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(InvisibleTypeAnno, annos.get(0).getAnnotationType().asElement());

        annos = p3.getAnnotationMirrors();
        assertEquals(0, annos.size());

        annos = p4.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(VisibleTypeAnno, annos.get(0).getAnnotationType().asElement());
    }

    @Test
    void onInnerClassConstructorParameter() throws Exception {
        TypeElementImpl AnnotatedMethodParameter = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedMethodParameter", null);
        ExecutableElementImpl ctor = AnnotatedMethodParameter.getMethod("<init>",
                "(Lpkg/Annotations;Ljava/lang/Object;)V");

        assertNotNull(ctor);

        assertEquals(1, ctor.getParameters().size());
        VariableElementImpl p1 = ctor.getParameters().get(0);

        List<AnnotationMirrorImpl> annos = p1.getAnnotationMirrors();
        assertEquals(2, annos.size());
        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());
        assertSame(VisibleTypeAnno, annos.get(1).getAnnotationType().asElement());
    }

    @Test
    void onMethodParameterTypeVariable() throws Exception {
        TypeElementImpl AnnotatedMethodParameterTypeVariable = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedMethodParameterTypeVariable", null);
        ExecutableElementImpl method = AnnotatedMethodParameterTypeVariable.getMethod("method", "(Ljava/util/Set;)V");

        assertNotNull(method);

        assertEquals(1, method.getParameters().size());
        VariableElementImpl param = method.getParameters().get(0);

        List<AnnotationMirrorImpl> annos = param.getAnnotationMirrors();
        assertTrue(annos.isEmpty());

        TypeMirrorImpl t = param.asType();
        assertTrue(t instanceof DeclaredTypeImpl);
        DeclaredTypeImpl pt = (DeclaredTypeImpl) t;

        assertEquals(1, pt.getTypeArguments().size());
        TypeMirrorImpl paramTypeVar = pt.getTypeArguments().get(0);

        annos = paramTypeVar.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(InvisibleTypeAnno, annos.get(0).getAnnotationType().asElement());
    }

    @Test
    void onField() throws Exception {
        TypeElementImpl AnnotatedField = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedField", null);
        VariableElementImpl field = AnnotatedField.getField("simpleField");

        assertNotNull(field);

        List<AnnotationMirrorImpl> annos = field.getAnnotationMirrors();
        assertEquals(2, annos.size());

        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());
        assertSame(InvisibleTypeAnno, annos.get(1).getAnnotationType().asElement());

        annos = field.asType().getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(InvisibleTypeAnno, annos.get(0).getAnnotationType().asElement());
    }

    @Test
    void onFieldArray() throws Exception {
        TypeElementImpl AnnotatedField = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedField", null);
        VariableElementImpl field = AnnotatedField.getField("arrayField");

        VariableElement fld = ElementFilter
                .fieldsIn(annotations.analyze().elements().getTypeElement("pkg.Annotations.AnnotatedField")
                        .getEnclosedElements())
                .stream().filter(f -> f.getSimpleName().contentEquals("arrayField")).findFirst().get();

        List<? extends AnnotationMirror> as = fld.asType().getAnnotationMirrors();

        assertNotNull(field);

        List<AnnotationMirrorImpl> annos = field.getAnnotationMirrors();
        assertEquals(2, annos.size());
        assertSame(VisibleAnno, annos.get(0).getAnnotationType().asElement());
        assertSame(InvisibleTypeAnno, annos.get(1).getAnnotationType().asElement());

        TypeMirrorImpl fieldType = field.asType();
        assertTrue(fieldType instanceof ArrayTypeImpl);
        annos = fieldType.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(VisibleTypeAnno, annos.get(0).getAnnotationType().asElement());

        fieldType = ((ArrayTypeImpl) fieldType).getComponentType();
        assertTrue(fieldType instanceof PrimitiveTypeImpl);
        annos = fieldType.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(InvisibleTypeAnno, annos.get(0).getAnnotationType().asElement());
    }

    @Test
    @Disabled
    void onFieldArrayDimension() throws Exception {
        // TODO implement
    }

    @Test
    @Disabled
    void onMethodException() throws Exception {
        // TODO implement
    }

    @Test
    @Disabled
    void onMethodReturnType() throws Exception {
        // TODO implement
    }

    @Test
    @Disabled
    void onMethodDefaultValue() throws Exception {
        // TODO implement
        // is this even possible?
    }

    @Test
    @Disabled
    void onInnerClassType() throws Exception {
        // TODO implement
        // Base.@Annotated Inner field;
    }

    @Test
    void onMethodReceiverType() throws Exception {
        TypeElementImpl Annotations = (TypeElementImpl) u.getTypeByInternalNameFromModule("pkg/Annotations", null);
        assertNotNull(Annotations);

        TypeElementImpl AnnotatedReceiverType = (TypeElementImpl) u
                .getTypeByInternalNameFromModule("pkg/Annotations$AnnotatedReceiverType", null);
        assertNotNull(AnnotatedReceiverType);

        ExecutableElementImpl implicitReceiverCtor = AnnotatedReceiverType.getMethod("<init>", "(Lpkg/Annotations;)V");
        assertNotNull(implicitReceiverCtor);

        ExecutableElementImpl explicitReceiverCtor = AnnotatedReceiverType.getMethod("<init>", "(Lpkg/Annotations;D)V");
        assertNotNull(explicitReceiverCtor);

        ExecutableElementImpl explicitReceiverMethod = AnnotatedReceiverType.getMethod("method", "(I)V");
        assertNotNull(explicitReceiverMethod);

        ExecutableElementImpl implicitReceiverMethod = AnnotatedReceiverType.getMethod("method", "(D)V");
        assertNotNull(implicitReceiverMethod);

        TypeMirrorImpl receiverType = implicitReceiverCtor.getReceiverType();
        assertNotNull(receiverType);
        assertSame(Annotations, ((DeclaredTypeImpl) receiverType).asElement());

        receiverType = explicitReceiverCtor.getReceiverType();
        assertNotNull(receiverType);
        assertSame(Annotations, ((DeclaredTypeImpl) receiverType).asElement());
        List<AnnotationMirrorImpl> annos = receiverType.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(InvisibleTypeAnno, annos.get(0).getAnnotationType().asElement());

        receiverType = implicitReceiverMethod.getReceiverType();
        assertNotNull(receiverType);
        assertSame(AnnotatedReceiverType, ((DeclaredTypeImpl) receiverType).asElement());

        receiverType = explicitReceiverMethod.getReceiverType();
        assertNotNull(receiverType);
        assertSame(AnnotatedReceiverType, ((DeclaredTypeImpl) receiverType).asElement());
        annos = receiverType.getAnnotationMirrors();
        assertEquals(1, annos.size());
        assertSame(VisibleTypeAnno, annos.get(0).getAnnotationType().asElement());
    }
}
