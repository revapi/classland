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
package org.revapi.classland.impl.model.mirror;

import static java.util.Collections.emptyList;

import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.revapi.classland.PrettyPrinting;
import org.revapi.classland.impl.TypeLookup;
import org.revapi.classland.impl.model.BaseModelImpl;
import org.revapi.classland.impl.model.element.ExecutableElementBase;
import org.revapi.classland.impl.model.element.ExecutableElementImpl;
import org.revapi.classland.impl.model.element.MissingExecutableElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.VariableElementImpl;

public final class AnnotationMirrorImpl extends BaseModelImpl implements AnnotationMirror {
    private final DeclaredTypeImpl annotationType;
    private final Map<ExecutableElementBase, AnnotationValueImpl> values;

    public AnnotationMirrorImpl(AnnotationNode node, TypeLookup lookup, TypeElementBase annotationType) {
        super(lookup);
        this.annotationType = annotationType.asType();

        if (node.values == null) {
            values = Collections.emptyMap();
        } else {
            values = new LinkedHashMap<>(node.values.size(), 1);
            boolean processingName = true;
            String name = "";
            for (Object v : node.values) {
                if (processingName) {
                    name = (String) v;
                    processingName = false;
                } else {
                    AnnotationValueImpl av = AnnotationValueImpl.fromAsmValue(lookup, v, annotationType,
                            annotationType.lookupModule());
                    List<ExecutableElementImpl> m = annotationType.getMethod(name);
                    if (m.isEmpty()) {
                        MissingExecutableElementImpl mm = new MissingExecutableElementImpl(lookup, annotationType, name,
                                deduceTypeFromAnnotationValue(av), emptyList());
                        values.put(mm, av);
                    } else {
                        values.put(m.get(0), av);
                    }
                    processingName = true;
                }
            }
        }
    }

    @Override
    public DeclaredTypeImpl getAnnotationType() {
        return annotationType;
    }

    @Override
    public Map<ExecutableElementBase, AnnotationValueImpl> getElementValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }

        AnnotationMirrorImpl that = (AnnotationMirrorImpl) o;

        if (!annotationType.equals(that.annotationType)) {
            return false;
        }
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + annotationType.hashCode();
        result = 31 * result + values.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return PrettyPrinting.print(new StringWriter(), this).toString();
    }

    private static String deduceTypeFromAnnotationValue(AnnotationValueImpl value) {
        AnnotationValueImpl.Kind kind = AnnotationValueImpl.Kind.of(value.getValue());
        switch (kind) {
        case ARRAY:
            if (((List<?>) value.getValue()).isEmpty()) {
                return "[Ljava/lang/Object;";
            } else {
                // noinspection unchecked
                return "[" + deduceTypeFromAnnotationValue(((List<AnnotationValueImpl>) value.getValue()).get(0));
            }
        case LONG:
            return Type.LONG_TYPE.getDescriptor();
        case ENUM:
            TypeElementBase t = (TypeElementBase) ((VariableElementImpl) value.getValue()).getEnclosingElement();
            return Type.getObjectType(t.getInternalName()).getDescriptor();
        case INT:
            return Type.INT_TYPE.getDescriptor();
        case BOOLEAN:
            return Type.BOOLEAN_TYPE.getDescriptor();
        case STRING:
            return "Ljava/lang/String;";
        case TYPE:
            t = ((DeclaredTypeImpl) value.getValue()).asElement();
            return Type.getObjectType(t.getInternalName()).getDescriptor();
        case ANNO:
            t = ((AnnotationMirrorImpl) value.getValue()).annotationType.asElement();
            return Type.getObjectType(t.getInternalName()).getDescriptor();
        case BYTE:
            return Type.BYTE_TYPE.getDescriptor();
        case CHAR:
            return Type.CHAR_TYPE.getDescriptor();
        case DOUBLE:
            return Type.DOUBLE_TYPE.getDescriptor();
        case FLOAT:
            return Type.FLOAT_TYPE.getDescriptor();
        case SHORT:
            return Type.SHORT_TYPE.getDescriptor();
        default:
            throw new IllegalArgumentException("Unhandled annotation value kind: " + kind);
        }
    }
}
