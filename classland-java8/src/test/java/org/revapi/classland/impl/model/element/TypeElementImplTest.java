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
import javax.lang.model.util.Elements;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.archive.jar.JarFileArchive;
import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.TypePool;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.mirror.TypeVariableImpl;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
class TypeElementImplTest {
    @JarSources(root = "/src/model/element/", sources = { "pkg/names/Dollar$1.java", "pkg/names/Dollars.java" })
    CompiledJar names;

    @JarSources(root = "/src/model/element/", sources = { "pkg/Kinds.java" })
    CompiledJar kinds;

    @JarSources(root = "/src/model/element/", sources = { "pkg/Modifiers.java" })
    CompiledJar modifiers;

    @JarSources(root = "/src/model/element/", sources = { "pkg/Generics.java" })
    CompiledJar generics;

    @JarSources(root = "/src/model/element/", sources = { "pkg/InnerClasses.java" })
    CompiledJar innerClasses;

    @Test
    void qualifiedNames() throws Exception {
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(names.jarFile())));

        Elements javacElements = names.analyze().elements();
        TypeElement jDollars = javacElements.getTypeElement("pkg.names.Dollars");

        TypeLookup l = u.getLookup();

        TypeElementBase top = l.getTypeByInternalNameFromModule("pkg/names/Dollars", null);
        TypeElementBase member = l.getTypeByInternalNameFromModule("pkg/names/Dollars$Member$1", null);
        TypeElementBase anon = l.getTypeByInternalNameFromModule("pkg/names/Dollars$Member$2", null);
        TypeElementBase local = l
                .getTypeByInternalNameFromModule("pkg/names/Dollars$Member$2$1LocalInInitializer$InnerInLocal", null);

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
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(names.jarFile())));

        TypeLookup l = u.getLookup();

        TypeElementBase top = l.getTypeByInternalNameFromModule("pkg/names/Dollars", null);
        TypeElementBase member = l.getTypeByInternalNameFromModule("pkg/names/Dollars$Member$1", null);
        TypeElementBase anon = l.getTypeByInternalNameFromModule("pkg/names/Dollars$Member$2", null);
        TypeElementBase local = l
                .getTypeByInternalNameFromModule("pkg/names/Dollars$Member$2$1LocalInInitializer$InnerInLocal", null);

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
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(kinds.jarFile())));

        TypeLookup l = u.getLookup();

        TypeElementBase Class = l.getTypeByInternalNameFromModule("pkg/Kinds$Classes$Class", null);
        TypeElementBase Interface = l.getTypeByInternalNameFromModule("pkg/Kinds$Classes$Interface", null);
        TypeElementBase Enum = l.getTypeByInternalNameFromModule("pkg/Kinds$Classes$Enum", null);
        TypeElementBase Annotation = l.getTypeByInternalNameFromModule("pkg/Kinds$Classes$Annotation", null);

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
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(modifiers.jarFile())));

        TypeLookup l = u.getLookup();

        TypeElementBase Modifiers = l.getTypeByInternalNameFromModule("pkg/Modifiers", null);
        TypeElementBase Public = l.getTypeByInternalNameFromModule("pkg/Modifiers$Classes$Public", null);
        TypeElementBase Protected = l.getTypeByInternalNameFromModule("pkg/Modifiers$Classes$Protected", null);
        TypeElementBase Private = l.getTypeByInternalNameFromModule("pkg/Modifiers$Classes$Private", null);
        TypeElementBase Static = l.getTypeByInternalNameFromModule("pkg/Modifiers$Classes$Static", null);
        TypeElementBase Final = l.getTypeByInternalNameFromModule("pkg/Modifiers$Classes$Final", null);
        TypeElementBase StrictFp = l.getTypeByInternalNameFromModule("pkg/Modifiers$Classes$StrictFp", null);
        TypeElementBase Abstract = l.getTypeByInternalNameFromModule("pkg/Modifiers$Classes$Abstract", null);

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
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(names.jarFile())));

        TypeElementBase top = u.getLookup().getTypeByInternalNameFromModule("pkg/names/Dollars", null);

        assertNotNull(top);

        List<TypeElement> innerClasses = ElementFilter.typesIn(top.getEnclosedElements());

        assertEquals(2, innerClasses.size());
        assertTrue(
                innerClasses.stream().anyMatch(t -> "pkg.names.Dollars.Member$1".contentEquals(t.getQualifiedName())));
        assertTrue(innerClasses.stream().anyMatch(t -> "pkg.names.Dollars.Member".contentEquals(t.getQualifiedName())));
    }

    @Test
    void superClass() throws Exception {
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(generics.jarFile())));
        TypeLookup lookup = u.getLookup();

        TypeElementBase base = lookup.getTypeByInternalNameFromModule("pkg/Generics$Base", null);
        TypeElementBase genericSuperClass = lookup
                .getTypeByInternalNameFromModule("pkg/Generics$ConcreteWithGenericSuperClass", null);
        TypeElementBase genericWithGenericSuperclass = lookup
                .getTypeByInternalNameFromModule("pkg/Generics$GenericWithParamUsedInSuperClass", null);

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
        TypeMirrorImpl upperBound = v.getUpperBound();
        assertTrue(upperBound instanceof DeclaredType);

        DeclaredType dt = (DeclaredType) upperBound;
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

        // TODO add tests for superClass of java.lang.Object and interface types
    }

    @Test
    void interfaces() throws Exception {
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(generics.jarFile())));

        TypeElementBase genericWithGenericInterface = u.getLookup()
                .getTypeByInternalNameFromModule("pkg/Generics$GenericWithParamUsedInInterface", null);

        List<TypeMirrorImpl> ifaces = genericWithGenericInterface.getInterfaces();
        assertEquals(2, ifaces.size());

        DeclaredTypeImpl cloneableIface = (DeclaredTypeImpl) ifaces.get(0);
        DeclaredTypeImpl baseIface = (DeclaredTypeImpl) ifaces.get(1);

        assertEquals("java.lang.Cloneable", ((TypeElement) cloneableIface.asElement()).getQualifiedName().toString());

        assertEquals(1, baseIface.getTypeArguments().size());
        TypeVariableImpl tv = (TypeVariableImpl) baseIface.getTypeArguments().get(0);
        assertEquals("T", tv.asElement().getSimpleName().toString());
        assertEquals("java.lang.String",
                ((TypeElement) ((DeclaredTypeImpl) tv.getUpperBound()).asElement()).getQualifiedName().toString());
    }

    @Test
    void enclosingElement() throws Exception {
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(innerClasses.jarFile())));
        TypeLookup lookup = u.getLookup();

        TypeElementImpl InnerClasses = (TypeElementImpl) lookup.getTypeByInternalNameFromModule("pkg/InnerClasses",
                null);
        TypeElementImpl StaticMember = (TypeElementImpl) lookup
                .getTypeByInternalNameFromModule("pkg/InnerClasses$StaticMember", null);
        TypeElementImpl StaticAnonymous = (TypeElementImpl) lookup
                .getTypeByInternalNameFromModule("pkg/InnerClasses$StaticMember$1", null);
        TypeElementImpl StaticLocal = (TypeElementImpl) lookup
                .getTypeByInternalNameFromModule("pkg/InnerClasses$StaticMember$1Local", null);
        ExecutableElementImpl method = StaticMember.getMethod("method", "()V");

        assertEquals(lookup.getPackageInModule("pkg", lookup.getUnnamedModule()), InnerClasses.getEnclosingElement());

        assertSame(InnerClasses, StaticMember.getEnclosingElement());

        assertSame(StaticMember, StaticAnonymous.getEnclosingElement());

        assertSame(method, StaticLocal.getEnclosingElement());
    }

    @Test
    void testCRTP() throws Exception {
        TypePool u = new TypePool(false);
        u.registerArchive(new JarFileArchive(new JarFile(generics.jarFile())));

        TypeElementImpl CRTP = (TypeElementImpl) u.getLookup().getTypeByInternalNameFromModule("pkg/Generics$CRTP",
                null);

        List<TypeParameterElementImpl> typeParams = CRTP.getTypeParameters();
        assertEquals(1, typeParams.size());

        TypeParameterElementImpl typeParam = typeParams.get(0);
        List<TypeMirrorImpl> bounds = typeParam.getBounds();
        assertEquals(1, bounds.size());
        DeclaredTypeImpl bound = (DeclaredTypeImpl) bounds.get(0);
        List<TypeMirrorImpl> typeArgs = bound.getTypeArguments();
        assertEquals(1, typeArgs.size());
        TypeVariableImpl arg = (TypeVariableImpl) typeArgs.get(0);
        assertSame(bound, arg.getUpperBound());
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... vals) {
        return new HashSet<>(asList(vals));
    }
}
