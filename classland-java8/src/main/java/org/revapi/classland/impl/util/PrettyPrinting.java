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
package org.revapi.classland.impl.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;

import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.VariableElementImpl;
import org.revapi.classland.impl.model.mirror.AnnotationValueImpl;

public class PrettyPrinting {
    private static final TypeVisitor<Void, Writer> TYPE_TO_STRING = new SimpleTypeVisitor8<Void, Writer>() {
        final ElementVisitor<Void, Writer> TYPE_FQN = new SimpleElementVisitor8<Void, Writer>() {
            @Override
            public Void visitType(TypeElement e, Writer wrt) {
                try {
                    wrt.append(e.getQualifiedName());
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Failed to pretty print the element.", ex);
                }
                return null;
            }
        };

        @Override
        public Void visitDeclared(DeclaredType t, Writer wrt) {
            return TYPE_FQN.visit(t.asElement(), wrt);
        }

        @Override
        public Void visitError(ErrorType t, Writer wrt) {
            return visitDeclared(t, wrt);
        }
    };

    private PrettyPrinting() {
    }

    public static Writer print(Writer wrt, AnnotationValue val) {
        Object value = val.getValue();
        return printConstant(wrt, value);
    }

    public static Writer print(Writer wrt, AnnotationMirror mirror) {
        try {
            wrt.append("@");
            TYPE_TO_STRING.visit(mirror.getAnnotationType(), wrt);
            Map<? extends ExecutableElement, ? extends AnnotationValue> vals = mirror.getElementValues();

            if (vals.isEmpty()) {
                return wrt;
            }

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : vals.entrySet()) {
                wrt.append(e.getKey().getSimpleName());
                wrt.append("=");
                print(wrt, e.getValue());
            }
            return wrt;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to pretty print the element.", e);
        }
    }

    public static Writer print(Writer wrt, Element element) {
        // TODO implement
        return wrt;
    }

    public static Writer printConstant(Writer wrt, Object value) {
        try {
            if (value == null) {
                wrt.append("null");
                return wrt;
            }

            switch (AnnotationValueImpl.Kind.of(value)) {
            case TYPE:
                TYPE_TO_STRING.visit((TypeMirror) value, wrt);
                wrt.append(".class");
                break;
            case ENUM:
                TypeElementBase parent = (TypeElementBase) ((VariableElementImpl) value).getEnclosingElement();
                wrt.append(parent.getQualifiedName()).append(".").append(((VariableElementImpl) value).getSimpleName());
                break;
            case ARRAY:
                wrt.append("{");
                @SuppressWarnings("unchecked")
                Iterator<AnnotationValue> it = ((List<AnnotationValue>) value).iterator();
                if (it.hasNext()) {
                    print(wrt, it.next());
                }

                while (it.hasNext()) {
                    wrt.append(", ");
                    print(wrt, it.next());
                }
                wrt.append("}");
                break;
            case STRING:
                wrt.append("\"").append((String) value).append("\"");
                break;
            case ANNO:
                print(wrt, (AnnotationMirror) value);
                break;
            default:
                wrt.append(value.toString());
            }

            return wrt;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to pretty print the element.", e);
        }
    }
}
