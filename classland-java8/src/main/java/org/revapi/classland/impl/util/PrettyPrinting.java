package org.revapi.classland.impl.util;

import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.VariableElementImpl;
import org.revapi.classland.impl.model.mirror.AnnotationValueImpl;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import java.util.List;

public class PrettyPrinting {
    private static final TypeVisitor<Void, StringBuilder> TYPE_TO_STRING = new SimpleTypeVisitor8<Void, StringBuilder>() {
        final ElementVisitor<Void, StringBuilder> TYPE_FQN = new SimpleElementVisitor8<Void, StringBuilder>() {
            @Override
            public Void visitType(TypeElement e, StringBuilder sb) {
                sb.append(e.getQualifiedName());
                return null;
            }
        };

        @Override
        public Void visitDeclared(DeclaredType t, StringBuilder sb) {
            return TYPE_FQN.visit(t.asElement());
        }

        @Override
        public Void visitError(ErrorType t, StringBuilder sb) {
            return visitDeclared(t, null);
        }
    };

    private PrettyPrinting() {
    }

    public static StringBuilder print(StringBuilder sb, AnnotationValue val) {
        Object value = val.getValue();
        switch (AnnotationValueImpl.Kind.of(value)) {
            case TYPE:
                TYPE_TO_STRING.visit((TypeMirror) value, sb);
                sb.append(".class");
                break;
            case ENUM:
                TypeElementBase parent = (TypeElementBase) ((VariableElementImpl) value).getEnclosingElement();
                sb.append(parent.getQualifiedName()).append(".").append(((VariableElementImpl) value).getSimpleName());
                break;
            case ARRAY:
                sb.append("{");
                //noinspection unchecked
                for (AnnotationValue av : (List<AnnotationValue>) value) {
                    print(sb, av);
                }
                sb.append("}");
                break;
            case STRING:
                sb.append("\"").append((String) value).append("\"");
                break;
            case ANNO:
                print(sb, (AnnotationMirror) value);
                break;
            default:
                sb.append(value.toString());
        }

        return sb;
    }

    public static void print(StringBuilder sb, AnnotationMirror mirror) {
        // TODO implement
    }
}
