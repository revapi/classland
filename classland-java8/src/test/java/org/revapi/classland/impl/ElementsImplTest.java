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
package org.revapi.classland.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
class ElementsImplTest {

    @JarSources(root = "/src/impl/", sources = {"packages/a/A.java", "packages/b/B.java"})
    private CompiledJar packages;

    @JarSources(root = "/src/impl/types/orig/", sources = {"types/A.java", "types/B.java", "module-info.java"})
    private CompiledJar typesOrig;

    @JarSources(root = "/src/impl/types/copy/", sources = {"types/A.java", "types/B.java", "module-info.java"})
    private CompiledJar typesCopy;

    @JarSources(root = "/src/impl/", sources = {"annoattrdefaults/Anno.java", "annoattrdefaults/User.java"})
    private CompiledJar annoAttrDefaults;

    @JarSources(root = "/src/impl/", sources = {"deprecated/DeprecatedClass.java"})
    private CompiledJar deprecated;

    @JarSources(root = "/src/impl/", sources = {"members/Base.java", "members/Extended.java"})
    private CompiledJar members;

    @Test
    void testGetPackageElement() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(packages.jarFile())));

        Elements els = new ElementsImpl(u);

        PackageElement pkg = els.getPackageElement("packages.a");
        assertNotNull(pkg);
        assertTrue(pkg.getQualifiedName().contentEquals("packages.a"));

        pkg = els.getPackageElement("packages.b");
        assertNotNull(pkg);
        assertTrue(pkg.getQualifiedName().contentEquals("packages.b"));

        pkg = els.getPackageElement("packages.nonExistent");
        assertNull(pkg);
    }

    @Test
    void testGetTypeElement_noModules() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(typesOrig.jarFile())));

        Elements els = new ElementsImpl(u);

        // finds in normal circumstances
        TypeElement type = els.getTypeElement("types.A");
        assertNotNull(type);
        assertEquals("types.A", type.getQualifiedName().toString());

        type = els.getTypeElement("types.B");
        assertNotNull(type);
        assertEquals("types.B", type.getQualifiedName().toString());

        type = els.getTypeElement("not.there");
        assertNull(type);
    }

    @Test
    void testGetTypeElement_modules() throws Exception {
        Universe u = new Universe(true);
        u.registerArchive(new JarFileArchive(new JarFile(typesOrig.jarFile())));

        Elements els = new ElementsImpl(u);

        TypeElement type = els.getTypeElement("types.A");
        assertNotNull(type);
        assertEquals("types.A", type.getQualifiedName().toString());

        type = els.getTypeElement("types.B");
        assertNotNull(type);
        assertEquals("types.B", type.getQualifiedName().toString());

        type = els.getTypeElement("not.there");
        assertNull(type);
    }

    @Test
    void testGetTypeElement_modulesWithDuplicates() throws Exception {
        Universe u = new Universe(true);
        u.registerArchive(new JarFileArchive(new JarFile(typesOrig.jarFile())));
        u.registerArchive(new JarFileArchive(new JarFile(typesCopy.jarFile())));

        Elements els = new ElementsImpl(u);

        TypeElement type = els.getTypeElement("types.A");
        assertNull(type);

        type = els.getTypeElement("types.B");
        assertNull(type);

        type = els.getTypeElement("not.there");
        assertNull(type);
    }

    @Test
    void testGetElementValuesWithDefaults() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(annoAttrDefaults.jarFile())));

        Elements els = new ElementsImpl(u);

        TypeElement user = els.getTypeElement("annoattrdefaults.User");
        assertNotNull(user);

        List<? extends AnnotationMirror> annos = user.getAnnotationMirrors();
        assertNotNull(annos);
        assertEquals(1, annos.size());

        AnnotationMirror anno = annos.get(0);

        Map<? extends ExecutableElement, ? extends AnnotationValue> attrs = els.getElementValuesWithDefaults(anno);
        assertNotNull(attrs);
        assertEquals(4, attrs.size());

        Optional<? extends AnnotationValue> v = getAttributeValue(attrs, "value");
        assertTrue(v.isPresent());
        assertEquals(5, v.get().getValue());

        v = getAttributeValue(attrs, "intDefault");
        assertTrue(v.isPresent());
        assertEquals(42, v.get().getValue());

        v = getAttributeValue(attrs, "classDefault");
        assertTrue(v.isPresent());
        assertEquals("java.lang.Void.class", v.get().toString());

        v = getAttributeValue(attrs, "arrayDefault");
        assertTrue(v.isPresent());
        assertEquals("{42, 43}", v.get().toString());
    }

    @Test
    void isDeprecated() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(deprecated.jarFile())));

        Elements els = new ElementsImpl(u);

        TypeElement deprecatedClass = els.getTypeElement("deprecated.DeprecatedClass");
        assertNotNull(deprecatedClass);

        assertTrue(els.isDeprecated(deprecatedClass));

        List<? extends VariableElement> fields = ElementFilter.fieldsIn(deprecatedClass.getEnclosedElements());
        assertNotNull(fields);
        assertEquals(1, fields.size());

        VariableElement deprecatedField = fields.get(0);
        assertNotNull(deprecatedField);
        assertTrue(els.isDeprecated(deprecatedField));

        List<? extends ExecutableElement> methods = ElementFilter.methodsIn(deprecatedClass.getEnclosedElements());
        assertNotNull(methods);
        assertEquals(2, methods.size());

        ExecutableElement deprecatedMethod = methods.get(0);
        assertNotNull(deprecatedMethod);
        assertEquals("deprecatedMethod", deprecatedMethod.getSimpleName().toString());
        assertTrue(els.isDeprecated(deprecatedMethod));

        ExecutableElement deprecatedParameterMethod = methods.get(1);
        assertNotNull(deprecatedParameterMethod);
        assertEquals("deprecatedParameter", deprecatedParameterMethod.getSimpleName().toString());
        assertEquals(2, deprecatedParameterMethod.getParameters().size());
        assertTrue(els.isDeprecated(deprecatedParameterMethod.getParameters().get(0)));
        assertFalse(els.isDeprecated(deprecatedParameterMethod.getParameters().get(1)));
    }

    @Test
    void testGetBinaryName() {
        // TODO implement
    }

    @Test
    void testGetPackageOf() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(deprecated.jarFile())));

        Elements els = new ElementsImpl(u);

        TypeElement deprecatedClass = els.getTypeElement("deprecated.DeprecatedClass");
        assertNotNull(deprecatedClass);
        assertEquals("deprecated", els.getPackageOf(deprecatedClass).getQualifiedName().toString());

        List<? extends VariableElement> fields = ElementFilter.fieldsIn(deprecatedClass.getEnclosedElements());
        assertNotNull(fields);
        assertEquals(1, fields.size());

        VariableElement deprecatedField = fields.get(0);
        assertNotNull(deprecatedField);
        assertEquals("deprecated", els.getPackageOf(deprecatedField).getQualifiedName().toString());

        List<? extends ExecutableElement> methods = ElementFilter.methodsIn(deprecatedClass.getEnclosedElements());
        assertNotNull(methods);
        assertEquals(2, methods.size());

        ExecutableElement deprecatedParameterMethod = methods.get(1);
        assertNotNull(deprecatedParameterMethod);
        assertEquals("deprecatedParameter", deprecatedParameterMethod.getSimpleName().toString());
        assertEquals("deprecated", els.getPackageOf(deprecatedParameterMethod).getQualifiedName().toString());
        assertEquals(2, deprecatedParameterMethod.getParameters().size());
        assertEquals("deprecated", els.getPackageOf(deprecatedParameterMethod.getParameters().get(0)).getQualifiedName().toString());
    }

    @Test
    void testGetAllMembers() throws Exception {
        Universe u = new Universe(false);
        u.registerArchive(new JarFileArchive(new JarFile(members.jarFile())));

        ElementsImpl els = new ElementsImpl(u);

        TypeElement Extended = els.getTypeElement("members.Extended");

        List<? extends Element> members = els.getAllMembers(Extended);
        assertNotNull(members);
        assertEquals(33, members.size());

        List<? extends VariableElement> fields = ElementFilter.fieldsIn(members);
        List<? extends ExecutableElement> methods = ElementFilter.methodsIn(members);
        List<? extends ExecutableElement> ctors = ElementFilter.constructorsIn(members);
        List<? extends TypeElement> types = ElementFilter.typesIn(members);

        assertTrue(findBySimpleName("publicStaticFieldBase", fields).isPresent());
        assertTrue(findBySimpleName("protectedStaticFieldBase", fields).isPresent());
        assertTrue(findBySimpleName("publicStaticFieldExtended", fields).isPresent());
        assertTrue(findBySimpleName("protectedStaticFieldExtended", fields).isPresent());
        assertTrue(findBySimpleName("privateStaticFieldExtended", fields).isPresent());

        assertTrue(findBySimpleName("publicFieldBase", fields).isPresent());
        assertTrue(findBySimpleName("protectedFieldBase", fields).isPresent());
        assertTrue(findBySimpleName("publicFieldExtended", fields).isPresent());
        assertTrue(findBySimpleName("protectedFieldExtended", fields).isPresent());
        assertTrue(findBySimpleName("privateFieldExtended", fields).isPresent());

        assertTrue(findBySimpleName("publicStaticMethodBase", methods).isPresent());
        assertTrue(findBySimpleName("protectedStaticMethodBase", methods).isPresent());
        assertTrue(findBySimpleName("publicStaticMethodExtended", methods).isPresent());
        assertTrue(findBySimpleName("protectedStaticMethodExtended", methods).isPresent());
        assertTrue(findBySimpleName("privateStaticMethodExtended", methods).isPresent());

        assertTrue(findBySimpleName("publicMethodBase", methods).isPresent());
        assertTrue(findBySimpleName("protectedMethodBase", methods).isPresent());
        assertTrue(findBySimpleName("publicMethodExtended", methods).isPresent());
        assertTrue(findBySimpleName("protectedMethodExtended", methods).isPresent());
        assertTrue(findBySimpleName("privateMethodExtended", methods).isPresent());

        assertTrue(findBySimpleName("PublicInnerClassBase", types).isPresent());
        assertTrue(findBySimpleName("ProtectedInnerClassBase", types).isPresent());
        assertTrue(findBySimpleName("PublicInnerClassExtended", types).isPresent());
        assertTrue(findBySimpleName("ProtectedInnerClassExtended", types).isPresent());
        assertTrue(findBySimpleName("PrivateInnerClassExtended", types).isPresent());
        
        assertTrue(findBySimpleName("PublicInnerStaticClassBase", types).isPresent());
        assertTrue(findBySimpleName("ProtectedInnerStaticClassBase", types).isPresent());
        assertTrue(findBySimpleName("PublicInnerStaticClassExtended", types).isPresent());
        assertTrue(findBySimpleName("ProtectedInnerStaticClassExtended", types).isPresent());
        assertTrue(findBySimpleName("PrivateInnerStaticClassExtended", types).isPresent());

        assertTrue(ctors.stream().anyMatch(c -> "void members.Extended::<init>()".equals(c.toString())));
        assertTrue(ctors.stream().anyMatch(c -> "void members.Extended::<init>(java.lang.Void)".equals(c.toString())));
        assertTrue(ctors.stream().anyMatch(c -> "void members.Extended::<init>(java.lang.Cloneable)".equals(c.toString())));
    }

    @Test
    void testGetAllAnnotationMirrors() {
        // TODO implement
    }

    @Test
    void testHides_types() {
        // TODO implement
    }

    @Test
    void testHides_fields() {
        // TODO implement
    }

    @Test
    void testHides_methods() {
        // TODO implement
    }

    @Test
    void testOverrides() {
        // TODO implement
    }

    @Test
    void testGetConstantExpression() {
        // TODO implement
    }

    @Test
    void testPrintElements() {
        // TODO implement
    }

    @Test
    void testGetName() {
        // TODO implement
    }

    @Test
    void testIsFunctionalInterface() {
        // TODO implement
    }

    private Optional<? extends AnnotationValue> getAttributeValue(
            Map<? extends ExecutableElement, ? extends AnnotationValue> attributes, String attributeName) {
        return attributes.entrySet().stream()
                .filter(e -> attributeName.contentEquals(e.getKey().getSimpleName()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private <T extends Element> Optional<T> findBySimpleName(String simpleName, List<T> elements) {
        return elements.stream().filter(e -> e.getSimpleName().contentEquals(simpleName)).findFirst();
    }
}
