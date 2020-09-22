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

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.mirror.TypeVariableImpl;
import org.revapi.classland.module.BaseModule;
import org.revapi.classland.module.JarFileModuleSource;
import org.revapi.classland.module.JarFileModuleSourceTest;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
class TypeElementImplTest {
    @JarSources(root = "/src/", sources = { "pkg/names/Dollar$1.java", "pkg/names/Dollars.java" })
    CompiledJar names;

    @JarSources(root = "/src/", sources = { "pkg/Kinds.java" })
    CompiledJar kinds;

    @JarSources(root = "/src/", sources = { "pkg/Modifiers.java" })
    CompiledJar modifiers;

    @JarSources(root = "/src/", sources = { "pkg/Generics.java" })
    CompiledJar generics;

    @JarSources(root = "/src/", sources = { "pkg/InnerClasses.java" })
    CompiledJar innerClasses;

    @Test
    void qualifiedNames() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(names.jarFile())));

        TypeElementBase top = u.getTypeByInternalName("pkg/names/Dollars");
        TypeElementBase member = u.getTypeByInternalName("pkg/names/Dollars$Member$1");
        TypeElementBase anon = u.getTypeByInternalName("pkg/names/Dollars$Member$2");
        TypeElementBase local = u.getTypeByInternalName("pkg/names/Dollars$Member$2$1LocalInInitializer$InnerInLocal");

        assertNotNull(top);
        assertNotNull(member);
        assertNotNull(anon);
        assertNotNull(local);

        assertTrue("pkg.names.Dollars".contentEquals(top.getQualifiedName()));
        assertTrue("pkg.names.Dollars.Member$1".contentEquals(member.getQualifiedName()));
        assertEquals(0, local.getQualifiedName().length());
        assertEquals(0, anon.getQualifiedName().length());
    }

    @Test
    void simpleNames() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(names.jarFile())));

        TypeElementBase top = u.getTypeByInternalName("pkg/names/Dollars");
        TypeElementBase member = u.getTypeByInternalName("pkg/names/Dollars$Member$1");
        TypeElementBase anon = u.getTypeByInternalName("pkg/names/Dollars$Member$2");
        TypeElementBase local = u.getTypeByInternalName("pkg/names/Dollars$Member$2$1LocalInInitializer$InnerInLocal");

        assertNotNull(top);
        assertNotNull(member);
        assertNotNull(anon);
        assertNotNull(local);

        assertTrue("Dollars".contentEquals(top.getSimpleName()));
        assertTrue("Member$1".contentEquals(member.getSimpleName()));
        assertTrue("InnerInLocal".contentEquals(local.getSimpleName()));
        assertEquals(0, anon.getSimpleName().length());
    }

    @Test
    void elementKind() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(kinds.jarFile())));

        TypeElementBase Class = u.getTypeByInternalName("pkg/Kinds$Classes$Class");
        TypeElementBase Interface = u.getTypeByInternalName("pkg/Kinds$Classes$Interface");
        TypeElementBase Enum = u.getTypeByInternalName("pkg/Kinds$Classes$Enum");
        TypeElementBase Annotation = u.getTypeByInternalName("pkg/Kinds$Classes$Annotation");

        assertNotNull(Class);
        assertNotNull(Interface);
        assertNotNull(Enum);
        assertNotNull(Annotation);

        assertEquals(ElementKind.CLASS, Class.getKind());
        assertEquals(ElementKind.INTERFACE, Interface.getKind());
        assertEquals(ElementKind.ENUM, Enum.getKind());
        assertEquals(ElementKind.ANNOTATION_TYPE, Annotation.getKind());
    }

    @Test
    void modifiers() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(modifiers.jarFile())));

        TypeElementBase Modifiers = u.getTypeByInternalName("pkg/Modifiers");
        TypeElementBase Public = u.getTypeByInternalName("pkg/Modifiers$Classes$Public");
        TypeElementBase Protected = u.getTypeByInternalName("pkg/Modifiers$Classes$Protected");
        TypeElementBase Private = u.getTypeByInternalName("pkg/Modifiers$Classes$Private");
        TypeElementBase Static = u.getTypeByInternalName("pkg/Modifiers$Classes$Static");
        TypeElementBase Final = u.getTypeByInternalName("pkg/Modifiers$Classes$Final");
        TypeElementBase StrictFp = u.getTypeByInternalName("pkg/Modifiers$Classes$StrictFp");
        TypeElementBase Abstract = u.getTypeByInternalName("pkg/Modifiers$Classes$Abstract");

        assertNotNull(Modifiers);
        assertNotNull(Public);
        assertNotNull(Protected);
        assertNotNull(Private);
        assertNotNull(Static);
        assertNotNull(Final);
        assertNotNull(StrictFp);
        assertNotNull(Abstract);

        assertEquals(asSet(Modifier.PUBLIC, Modifier.ABSTRACT), Modifiers.getModifiers());
        assertEquals(singleton(Modifier.PUBLIC), Public.getModifiers());
        assertEquals(singleton(Modifier.PROTECTED), Protected.getModifiers());
        assertEquals(singleton(Modifier.PRIVATE), Private.getModifiers());
        assertEquals(singleton(Modifier.STATIC), Static.getModifiers());
        assertEquals(singleton(Modifier.FINAL), Final.getModifiers());
        assertEquals(emptySet(), StrictFp.getModifiers()); // not set on class but on all its methods
        assertEquals(singleton(Modifier.ABSTRACT), Abstract.getModifiers());
    }

    @Test
    void innerClassDollarNames() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(names.jarFile())));

        TypeElementBase top = u.getTypeByInternalName("pkg/names/Dollars");

        assertNotNull(top);

        List<TypeElement> innerClasses = ElementFilter.typesIn(top.getEnclosedElements());

        assertEquals(2, innerClasses.size());
        assertTrue(
                innerClasses.stream().anyMatch(t -> "pkg.names.Dollars.Member$1".contentEquals(t.getQualifiedName())));
        assertTrue(innerClasses.stream().anyMatch(t -> "pkg.names.Dollars.Member".contentEquals(t.getQualifiedName())));
    }

    @Test
    void superClass() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(generics.jarFile())));

        TypeElementBase base = u.getTypeByInternalName("pkg/Generics$Base");
        TypeElementBase genericSuperClass = u.getTypeByInternalName("pkg/Generics$ConcreteWithGenericSuperClass");
        TypeElementBase genericWithGenericSuperclass = u
                .getTypeByInternalName("pkg/Generics$GenericWithParamUsedInSuperClass");

        assertNotNull(base);
        assertNotNull(genericSuperClass);
        assertNotNull(genericWithGenericSuperclass);

        TypeMirrorImpl superClass = genericWithGenericSuperclass.getSuperclass();
        assertNotNull(superClass);

        assertTrue(superClass instanceof DeclaredTypeImpl);
        DeclaredTypeImpl sc = (DeclaredTypeImpl) superClass;

        assertSame(base, sc.asElement());
        assertEquals(1, sc.getTypeArguments().size());

        TypeMirrorImpl arg = sc.getTypeArguments().get(0);
        assertTrue(arg instanceof TypeVariableImpl);

        TypeVariableImpl v = ((TypeVariableImpl) arg);
        TypeMirrorImpl lowerBound = v.getLowerBound();
        assertTrue(lowerBound instanceof DeclaredType);

        DeclaredType dt = (DeclaredType) lowerBound;
        assertTrue("java.lang.Number".contentEquals(((TypeElement) dt.asElement()).getQualifiedName()));
        assertEquals(TypeKind.ERROR, dt.getKind()); // we're not loading types from the base module...

        superClass = genericSuperClass.getSuperclass();
        assertNotNull(superClass);

        assertTrue(superClass instanceof DeclaredTypeImpl);
        sc = (DeclaredTypeImpl) superClass;

        assertSame(base, sc.asElement());
        assertEquals(1, sc.getTypeArguments().size());

        arg = sc.getTypeArguments().get(0);
        assertTrue(arg instanceof DeclaredTypeImpl);
        assertTrue("java.lang.String"
                .contentEquals(((TypeElement) ((DeclaredTypeImpl) arg).asElement()).getQualifiedName()));
        assertEquals(TypeKind.ERROR, arg.getKind()); // we're not loading types from the base module...
    }

    @Test
    void interfaces() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(generics.jarFile())));

        TypeElementBase genericWithGenericInterface = u
                .getTypeByInternalName("pkg/Generics$GenericWithParamUsedInInterface");

        List<TypeMirrorImpl> ifaces = genericWithGenericInterface.getInterfaces();
        assertEquals(2, ifaces.size());

        DeclaredTypeImpl cloneableIface = (DeclaredTypeImpl) ifaces.get(0);
        DeclaredTypeImpl baseIface = (DeclaredTypeImpl) ifaces.get(1);

        assertEquals("java.lang.Cloneable", ((TypeElement) cloneableIface.asElement()).getQualifiedName().toString());

        assertEquals(1, baseIface.getTypeArguments().size());
        TypeVariableImpl tv = (TypeVariableImpl) baseIface.getTypeArguments().get(0);
        assertEquals("T", tv.asElement().getSimpleName().toString());
        assertEquals("java.lang.String",
                ((TypeElement) ((DeclaredTypeImpl) tv.getLowerBound()).asElement()).getQualifiedName().toString());
    }

    @Test
    void enclosingElement() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(innerClasses.jarFile())));

        TypeElementImpl InnerClasses = (TypeElementImpl) u.getTypeByInternalName("pkg/InnerClasses");
        TypeElementImpl StaticMember = (TypeElementImpl) u.getTypeByInternalName("pkg/InnerClasses$StaticMember");
        TypeElementImpl StaticAnonymous = (TypeElementImpl) u.getTypeByInternalName("pkg/InnerClasses$StaticMember$1");
        TypeElementImpl StaticLocal = (TypeElementImpl) u.getTypeByInternalName("pkg/InnerClasses$StaticMember$1Local");
        ExecutableElementImpl method = StaticMember.getMethod("method", "()V");

        assertSame(u.getPackage("pkg"), InnerClasses.getEnclosingElement());

        assertSame(InnerClasses, StaticMember.getEnclosingElement());

        assertSame(StaticMember, StaticAnonymous.getEnclosingElement());

        assertSame(method, StaticLocal.getEnclosingElement());
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... vals) {
        return new HashSet<>(asList(vals));
    }
}
