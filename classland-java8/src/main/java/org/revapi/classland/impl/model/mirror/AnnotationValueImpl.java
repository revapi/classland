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
package org.revapi.classland.impl.model.mirror;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.BaseModelImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.VariableElementImpl;
import org.revapi.classland.impl.model.signature.SignatureParser;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.PrettyPrinting;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Nullable;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class AnnotationValueImpl extends BaseModelImpl implements AnnotationValue {

    private final Object value;

    public AnnotationValueImpl(Universe universe, Object value) {
        super(universe);
        this.value = value;
    }

    public static AnnotationValueImpl fromAsmValue(Universe universe, Object value, TypeVariableResolutionContext resolutionContext, Memoized<@Nullable ModuleElementImpl> typeLookupSource) {
        if (value instanceof Type) {
            // class value
            value = TypeMirrorFactory.create(universe, SignatureParser.parseInternalName(((Type) value).getInternalName()),
                    resolutionContext, AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, typeLookupSource);
        } else if (value instanceof String[]) {
            // enum constants
            // the first element is the descriptor of the enum class, the second element is the name of the field
            String enumTypeDescriptor = ((String[]) value)[0];
            String enumConstantName = ((String[]) value)[1];

            TypeElementBase enumType = universe.getTypeByInternalNameFromModule(
                    Type.getType(enumTypeDescriptor).getInternalName(), typeLookupSource.get());

            value = enumType.getField(enumConstantName);
            if (value == null) {
                value = new VariableElementImpl.Missing(universe, enumType, enumConstantName, "L" + enumType.getInternalName() + ";", ElementKind.ENUM_CONSTANT);
            }
        } else if (value instanceof AnnotationNode) {
            // annotation
            value = new AnnotationMirrorImpl((AnnotationNode) value, universe, universe.getTypeByInternalNameFromModule(Type.getType(((AnnotationNode) value).desc).getInternalName(), typeLookupSource.get()));
        } else if (value instanceof List) {
            // array of values

            //noinspection unchecked
            value = ((List<Object>) value).stream().map(v -> fromAsmValue(universe, v, resolutionContext, typeLookupSource)).collect(toList());
        }

        return new AnnotationValueImpl(universe, value);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return PrettyPrinting.print(new StringBuilder(), this).toString();
    }

    @Override
    public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
        switch (Kind.of(value)) {
        case BOOLEAN:
            return v.visitBoolean((Boolean) value, p);
        case BYTE:
            return v.visitByte((Byte) value, p);
        case SHORT:
            return v.visitShort((Short) value, p);
        case INT:
            return v.visitInt((Integer) value, p);
        case LONG:
            return v.visitLong((Long) value, p);
        case FLOAT:
            return v.visitFloat((Float) value, p);
        case DOUBLE:
            return v.visitDouble((Double) value, p);
        case CHAR:
            return v.visitChar((Character) value, p);
        case STRING:
            return v.visitString((String) value, p);
        case TYPE:
            return v.visitType((TypeMirror) value, p);
        case ENUM:
            return v.visitEnumConstant((VariableElement) value, p);
        case ANNO:
            return v.visitAnnotation((AnnotationMirror) value, p);
        case ARRAY:
            // noinspection unchecked
            return v.visitArray((List<? extends AnnotationValue>) value, p);
        default:
            throw new IllegalStateException("Unsupported annotation value: " + value + ". This is a bug.");
        }
    }

    public enum Kind {
        BOOLEAN(Boolean.class), BYTE(Byte.class), SHORT(Short.class), INT(Integer.class), LONG(Long.class), FLOAT(Float.class), DOUBLE(Double.class), CHAR(Character.class), STRING(String.class), TYPE(TypeMirror.class), ENUM(VariableElement.class), ANNO(AnnotationMirror.class), ARRAY(List.class);

        private final Class<?> type;

        Kind(Class<?> type) {
            this.type = type;
        }

        public static Kind of(Object value) {
            for (Kind k : Kind.values()) {
                if (k.type.isAssignableFrom(value.getClass())) {
                    return k;
                }
            }

            throw new IllegalStateException("Unsupported annotation value: " + value + ". This is a bug.");
        }
    }
}
