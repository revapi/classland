package org.revapi.classland.impl.model.element;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.module.JarFileModuleSource;
import org.revapi.testjars.CompiledJar;
import org.revapi.testjars.junit5.CompiledJarExtension;
import org.revapi.testjars.junit5.JarSources;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CompiledJarExtension.class)
public class VariableElementImplFieldTest {
    @JarSources(root = "/src/", sources = {"pkg/Fields.java"})
    CompiledJar fields;

    @Test
    void constantValue() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(fields.jarFile())));

        TypeElementImpl Fields = u.getTypeByInternalName("pkg/Fields").orElse(null);
        assertNotNull(Fields);

        List<VariableElement> fields = ElementFilter.fieldsIn(Fields.getEnclosedElements());

        VariableElement staticWithoutValue = fields.stream()
                .filter(f -> "staticWithoutValue".contentEquals(f.getSimpleName()))
                .findFirst()
                .orElse(null);
        VariableElement staticWithValue = fields.stream()
                .filter(f -> "staticWithValue".contentEquals(f.getSimpleName()))
                .findFirst()
                .orElse(null);

        assertNotNull(staticWithoutValue);
        assertNotNull(staticWithValue);

        assertNull(staticWithoutValue.getConstantValue());
        assertEquals(2, staticWithValue.getConstantValue());
    }

    @Test
    void enumConstant() throws Exception {
        Universe u = new Universe();
        u.registerModule(new JarFileModuleSource(new JarFile(fields.jarFile())));

        TypeElementImpl Enum = u.getTypeByInternalName("pkg/Fields$Enum").orElse(null);
        assertNotNull(Enum);

        List<VariableElement> fields = ElementFilter.fieldsIn(Enum.getEnclosedElements());

        assertEquals(2, fields.size());
        assertTrue(fields.stream()
                .anyMatch(f -> f.getKind() == ElementKind.ENUM_CONSTANT
                        && "VARIANT1".contentEquals(f.getSimpleName())
                        && new HashSet<>(asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
                        .equals(f.getModifiers())));
        assertTrue(fields.stream()
                .anyMatch(f -> f.getKind() == ElementKind.FIELD
                        && "normalField".contentEquals(f.getSimpleName())
                        && new HashSet<>(asList(Modifier.STATIC, Modifier.FINAL)).equals(f.getModifiers())));
    }
}
