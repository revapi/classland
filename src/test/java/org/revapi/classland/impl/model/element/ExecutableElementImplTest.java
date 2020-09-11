package org.revapi.classland.impl.model.element;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.module.JarFileModuleSource;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
public class ExecutableElementImplTest {
    @JarSources(root = "/src/", sources = {"pkg/Methods.java"})
    CompiledJar methods;

    @Test
    void defaultMethods() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(methods.jarFile())));

        TypeElementImpl DefaultMethods = u.getTypeByInternalName("pkg/Methods$DefaultMethods").orElse(null);

        Assertions.assertNotNull(DefaultMethods);

        List<ExecutableElement> methods = ElementFilter.methodsIn(DefaultMethods.getEnclosedElements());

        assertEquals(2, methods.size());
        assertTrue(methods.stream().anyMatch(m -> !m.isDefault()));
        assertTrue(methods.stream().anyMatch(ExecutableElement::isDefault));
    }

    @Test
    void elementKinds() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(methods.jarFile())));

        TypeElementImpl ElementKinds = u.getTypeByInternalName("pkg/Methods$ElementKinds").orElse(null);

        Assertions.assertNotNull(ElementKinds);

        List<? extends Element> enclosed = ElementKinds.getEnclosedElements();

        assertEquals(3, enclosed.size());

        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.CONSTRUCTOR).count());
        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.METHOD).count());
        assertEquals(1, enclosed.stream().filter(e -> e.getKind() == ElementKind.STATIC_INIT).count());
    }
}
