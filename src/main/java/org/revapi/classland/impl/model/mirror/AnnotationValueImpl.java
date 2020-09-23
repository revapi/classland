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
import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.revapi.classland.impl.model.BaseModelImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.util.Memoized;

public class AnnotationValueImpl extends BaseModelImpl implements AnnotationValue {
    private final Memoized<Object> getValue;
    private final Kind kind;

    public AnnotationValueImpl(Universe universe, Supplier<Object> getValue, Kind kind) {
        super(universe);
        this.getValue = Memoized.memoize(getValue);
        this.kind = kind;
    }

    @Override
    public Object getValue() {
        return getValue.get();
    }

    @Override
    public String toString() {
        // TODO implement - this is required to be implemented by AnnotationValue in a particular way
        return "AnnotationValueImpl{}";
    }

    @Override
    public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
        Object value = getValue.get();
        switch (kind) {
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
            throw new IllegalStateException("Unsupported annotation value kind: " + kind + ". This is a bug.");
        }
    }

    private enum Kind {
        BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR, STRING, TYPE, ENUM, ANNO, ARRAY
    }
}
