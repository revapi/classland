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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleElementVisitor7;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor7;
import javax.lang.model.util.SimpleTypeVisitor8;

import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.VariableElementImpl;
import org.revapi.classland.impl.model.mirror.AnnotationValueImpl;

/**
 * This class is pretty much copied from Revapi.
 */
public class BasePrettyPrinting {
    private static final SimpleTypeVisitor8<Name, Void> TYPE_VARIABLE_NAME = new SimpleTypeVisitor8<Name, Void>() {
        @Override
        protected Name defaultAction(TypeMirror e, Void ignore) {
            return null;
        }

        @Override
        public Name visitTypeVariable(TypeVariable t, Void ignored) {
            return t.asElement().getSimpleName();
        }
    };

    private static final SimpleTypeVisitor8<Boolean, Void> IS_JAVA_LANG_OBJECT = new SimpleTypeVisitor8<Boolean, Void>() {
        @Override
        protected Boolean defaultAction(TypeMirror e, Void aVoid) {
            return false;
        }

        @Override
        public Boolean visitDeclared(DeclaredType t, Void aVoid) {
            Element el = t.asElement();
            if (el instanceof TypeElement) {
                return ((TypeElement) el).getQualifiedName().contentEquals("java.lang.Object");
            }

            return false;
        }

    };

    private static final TypeVisitor<Void, StringBuilderAndState> MIRROR_TO_STRING = new DepthTrackingVisitor<Void>() {

        @Override
        protected Void doVisitPrimitive(PrimitiveType t, StringBuilderAndState state) {
            switch (t.getKind()) {
            case BOOLEAN:
                state.bld.append("boolean");
                break;
            case BYTE:
                state.bld.append("byte");
                break;
            case CHAR:
                state.bld.append("char");
                break;
            case DOUBLE:
                state.bld.append("double");
                break;
            case FLOAT:
                state.bld.append("float");
                break;
            case INT:
                state.bld.append("int");
                break;
            case LONG:
                state.bld.append("long");
                break;
            case SHORT:
                state.bld.append("short");
                break;
            default:
                break;
            }

            return null;
        }

        @Override
        protected Void doVisitArray(ArrayType t, StringBuilderAndState state) {
            t.getComponentType().accept(this, state);
            state.bld.append("[]");
            return null;
        }

        @Override
        protected Void doVisitTypeVariable(TypeVariable t, StringBuilderAndState state) {
            Name tName = t.asElement().getSimpleName();
            if (state.depth > state.anticipatedTypeVarDeclDepth && state.forwardTypeVarDecls.contains(tName)) {
                state.bld.append(tName);
                return null;
            }

            state.bld.append(tName);

            TypeMirror lowerBound = t.getLowerBound();

            if (lowerBound != null && lowerBound.getKind() != TypeKind.NULL) {
                state.bld.append(" super ");
                lowerBound.accept(this, state);
            }

            TypeMirror upperBound = t.getUpperBound();

            if (!IS_JAVA_LANG_OBJECT.visit(upperBound)) {
                state.bld.append(" extends ");
                upperBound.accept(this, state);
            }

            return null;
        }

        @Override
        protected Void doVisitWildcard(WildcardType t, StringBuilderAndState state) {
            state.bld.append("?");

            TypeMirror superBound = t.getSuperBound();
            if (superBound != null) {
                state.bld.append(" super ");
                superBound.accept(this, state);
            }

            TypeMirror extendsBound = t.getExtendsBound();
            if (extendsBound != null) {
                state.bld.append(" extends ");
                extendsBound.accept(this, state);
            }

            return null;
        }

        @Override
        protected Void doVisitExecutable(ExecutableType t, StringBuilderAndState state) {
            Runnable methodNameRenderer = null;
            if (state.methodInitAndNameOutput != null) {
                methodNameRenderer = state.methodInitAndNameOutput.apply(state);
            }

            int currentTypeDeclDepth = state.anticipatedTypeVarDeclDepth;
            state.anticipatedTypeVarDeclDepth = state.depth + 1;
            List<? extends TypeVariable> typeVars = t.getTypeVariables();
            visitTypeVars(typeVars, state);
            List<Name> typeVarNames = typeVars.stream().map(v -> v.asElement().getSimpleName())
                    .filter(v -> !state.forwardTypeVarDecls.contains(v)).collect(Collectors.toList());
            state.forwardTypeVarDecls.addAll(typeVarNames);
            state.anticipatedTypeVarDeclDepth = currentTypeDeclDepth;

            state.visitingMethod = true;

            if (!typeVars.isEmpty()) {
                state.bld.append(" ");
            }

            currentTypeDeclDepth = state.anticipatedTypeVarDeclDepth;
            state.anticipatedTypeVarDeclDepth = state.depth;

            t.getReturnType().accept(this, state);

            state.bld.append(" ");

            if (methodNameRenderer != null) {
                methodNameRenderer.run();
            }

            state.bld.append("(");

            Iterator<? extends TypeMirror> it = t.getParameterTypes().iterator();
            if (it.hasNext()) {
                it.next().accept(this, state);
            }
            while (it.hasNext()) {
                state.bld.append(", ");
                it.next().accept(this, state);
            }
            state.bld.append(")");

            List<? extends TypeMirror> thrownTypes = t.getThrownTypes();
            if (!thrownTypes.isEmpty()) {
                state.bld.append(" throws ");
                it = thrownTypes.iterator();

                it.next().accept(this, state);
                while (it.hasNext()) {
                    state.bld.append(", ");
                    it.next().accept(this, state);
                }
            }

            state.visitingMethod = false;
            state.forwardTypeVarDecls.removeAll(typeVarNames);
            state.anticipatedTypeVarDeclDepth = currentTypeDeclDepth;
            return null;
        }

        @Override
        protected Void doVisitNoType(NoType t, StringBuilderAndState state) {
            switch (t.getKind()) {
            case VOID:
                state.bld.append("void");
                break;
            case PACKAGE:
                state.bld.append("package");
                break;
            default:
                break;
            }

            return null;
        }

        @Override
        protected Void doVisitDeclared(DeclaredType t, StringBuilderAndState state) {
            int anticipatedTypeVarDeclDepth = state.anticipatedTypeVarDeclDepth;
            int depth = state.depth;

            CharSequence name;
            TypeElement type = (TypeElement) t.asElement();
            if (type.getNestingKind() == NestingKind.ANONYMOUS || type.getNestingKind() == NestingKind.LOCAL) {
                name = "<anonymous " + ((TypeElementBase) type).getInternalName().replace('/', '.') + ">";
            } else if (t.getEnclosingType().getKind() != TypeKind.NONE) {
                state.depth--; // we need to visit the parent with the same depth as this type
                visit(t.getEnclosingType(), state);
                state.depth = depth;
                state.anticipatedTypeVarDeclDepth = anticipatedTypeVarDeclDepth;
                ((DeclaredType) t.getEnclosingType()).getTypeArguments()
                        .forEach(a -> state.forwardTypeVarDecls.add(TYPE_VARIABLE_NAME.visit(a)));

                name = "." + t.asElement().getSimpleName();
            } else {
                name = type.getQualifiedName();
            }

            state.bld.append(name);

            if (state.depth == 1) {
                state.anticipatedTypeVarDeclDepth = 2;
            }
            visitTypeVars(t.getTypeArguments(), state);
            state.anticipatedTypeVarDeclDepth = anticipatedTypeVarDeclDepth;

            return null;
        }

        @Override
        protected Void doVisitIntersection(IntersectionType t, StringBuilderAndState state) {
            Iterator<? extends TypeMirror> it = t.getBounds().iterator();
            if (it.hasNext()) {
                it.next().accept(this, state);
            }

            TypeVisitor<Void, StringBuilderAndState> me = this;

            it.forEachRemaining(b -> {
                state.bld.append(" & ");
                b.accept(me, state);
            });

            return null;
        }

        @Override
        protected Void doVisitError(ErrorType t, StringBuilderAndState state) {
            state.bld.append(((TypeElement) t.asElement()).getQualifiedName());
            return null;
        }

        private boolean visitTypeVars(List<? extends TypeMirror> vars, StringBuilderAndState state) {
            if (!vars.isEmpty()) {
                Set<Name> names = vars.stream().map(v -> TYPE_VARIABLE_NAME.visit(v)).collect(Collectors.toSet());
                names.removeAll(state.forwardTypeVarDecls);
                state.forwardTypeVarDecls.addAll(names);
                try {
                    state.bld.append("<");
                    Iterator<? extends TypeMirror> it = vars.iterator();
                    it.next().accept(this, state);

                    while (it.hasNext()) {
                        state.bld.append(", ");
                        it.next().accept(this, state);
                    }

                    state.bld.append(">");
                } finally {
                    state.forwardTypeVarDecls.removeAll(names);
                }

                return true;
            }

            return false;
        }

    };

    private static final ElementVisitor<Void, StringBuilderAndState> ELEMENT_TO_STRING = new SimpleElementVisitor8<Void, StringBuilderAndState>() {
        @Override
        public Void visitUnknown(Element e, StringBuilderAndState state) {
            state.bld.append("<unknown ").append(e.getKind()).append(">");
            return null;
        }

        @Override
        public Void visitVariable(VariableElement e, StringBuilderAndState state) {
            Element enclosing = e.getEnclosingElement();
            if (enclosing instanceof TypeElement) {
                enclosing.accept(this, state);
                state.bld.append(".").append(e.getSimpleName());
            } else if (enclosing instanceof ExecutableElement) {
                // this means someone asked to directly output a string representation of a method parameter
                // in this case, we need to identify the parameter inside the full method signature so that
                // the full location is available.
                int paramIdx = ((ExecutableElement) enclosing).getParameters().indexOf(e);
                enclosing.accept(this, state);
                int openPar = state.bld.indexOf("(");
                int closePar = state.bld.indexOf(")", openPar);

                int paramStart = openPar + 1;
                int curParIdx = -1;
                int parsingState = 0; // 0 = normal, 1 = inside type param
                int typeParamDepth = 0;
                for (int i = openPar + 1; i < closePar; ++i) {
                    char c = state.bld.charAt(i);
                    switch (parsingState) {
                    case 0: // normal type
                        switch (c) {
                        case ',':
                            curParIdx++;
                            if (curParIdx == paramIdx) {
                                String par = state.bld.substring(paramStart, i);
                                state.bld.replace(paramStart, i, "===" + par + "===");
                            } else {
                                // accommodate for the space after commas for the second and further parameters
                                paramStart = i + (paramIdx == 0 ? 1 : 2);
                            }
                            break;
                        case '<':
                            parsingState = 1;
                            typeParamDepth = 1;
                            break;
                        }
                        break;
                    case 1: // inside type param
                        switch (c) {
                        case '<':
                            typeParamDepth++;
                            break;
                        case '>':
                            typeParamDepth--;
                            if (typeParamDepth == 0) {
                                parsingState = 0;
                            }
                            break;
                        }
                        break;
                    }
                }

                if (++curParIdx == paramIdx) {
                    String par = state.bld.substring(paramStart, closePar);
                    state.bld.replace(paramStart, closePar, "===" + par + "===");
                }
            }

            return null;
        }

        @Override
        public Void visitPackage(PackageElement e, StringBuilderAndState state) {
            state.bld.append(e.getQualifiedName());
            return null;
        }

        @Override
        public Void visitType(TypeElement e, StringBuilderAndState state) {
            return e.asType().accept(MIRROR_TO_STRING, state);
        }

        @Override
        public Void visitExecutable(ExecutableElement e, StringBuilderAndState state) {
            state.methodInitAndNameOutput = st -> {
                // we need to initialize the forward decls of the type here so that they are remembered for the rest
                // of the method rendering
                Element parent = e.getEnclosingElement();
                List<Name> names = new ArrayList<>(4);
                while (parent instanceof TypeElement) {
                    TypeElement type = (TypeElement) parent;
                    type.getTypeParameters().stream().map(p -> TYPE_VARIABLE_NAME.visit(p.asType()))
                            .forEach(names::add);

                    parent = parent.getEnclosingElement();
                }
                st.forwardTypeVarDecls.addAll(names);

                return () -> {
                    int depth = st.depth;
                    try {
                        // we're outputting the declaring type and that type might be declared with type params
                        // we need to reset the depth for this, so that the logic correctly expands type var decls
                        st.depth = 0;
                        e.getEnclosingElement().accept(this, st);

                        // we need to add the type param forward decls again, because they've been reset in the above
                        // rendering.
                        st.forwardTypeVarDecls.addAll(names);

                        st.bld.append("::").append(e.getSimpleName());
                    } finally {
                        st.depth = depth;
                    }
                };
            };

            e.asType().accept(MIRROR_TO_STRING, state);

            return null;
        }

        @Override
        public Void visitTypeParameter(TypeParameterElement e, StringBuilderAndState state) {
            return e.asType().accept(MIRROR_TO_STRING, state);
        }
    };

    protected BasePrettyPrinting() {
    }

    public static Writer print(Writer wrt, AnnotationValue val) {
        Object value = val.getValue();
        return printConstant(wrt, value);
    }

    public static Writer print(Writer wrt, AnnotationMirror mirror) {
        try {
            wrt.append("@");
            StringBuilderAndState state = new StringBuilderAndState();
            MIRROR_TO_STRING.visit(mirror.getAnnotationType(), state);
            wrt.append(state.bld);
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

    public static Writer print(Writer wrt, AnnotatedConstruct element) {
        StringBuilderAndState state = new StringBuilderAndState();
        if (element instanceof Element) {
            ELEMENT_TO_STRING.visit((Element) element, state);
        } else {
            MIRROR_TO_STRING.visit((TypeMirror) element, state);
        }
        silentWrite(element, wrt, state.bld);
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
                StringBuilderAndState state = new StringBuilderAndState();
                MIRROR_TO_STRING.visit((TypeMirror) value, state);
                wrt.append(state.bld).append(".class");
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

    protected static void silentWrite(Object element, Writer writer, CharSequence value) {
        try {
            writer.append(value);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to pretty print the element " + element, e);
        }
    }

    protected static void visitListUsing(TypeVisitor<Void, Writer> visitor, Object t, List<? extends TypeMirror> list,
            Writer writer) {
        if (list.isEmpty()) {
            return;
        }

        Iterator<? extends TypeMirror> it = list.iterator();
        visitor.visit(it.next(), writer);
        while (it.hasNext()) {
            silentWrite(t, writer, ", ");
            visitor.visit(it.next(), writer);
        }
    }

    private static class StringBuilderAndState {
        final StringBuilder bld = new StringBuilder();
        final Set<TypeMirror> visitedObjects = new HashSet<>(4);
        final Set<Name> forwardTypeVarDecls = new HashSet<>(2);
        Function<StringBuilderAndState, Runnable> methodInitAndNameOutput;
        boolean visitingMethod;
        int depth;
        int anticipatedTypeVarDeclDepth;
    }

    private static class DepthTrackingVisitor<T> extends SimpleTypeVisitor8<T, StringBuilderAndState> {
        @Override
        public final T visitIntersection(IntersectionType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitIntersection(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitIntersection(IntersectionType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitUnion(UnionType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitUnion(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitUnion(UnionType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitPrimitive(PrimitiveType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitPrimitive(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitPrimitive(PrimitiveType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitNull(NullType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitNull(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitNull(NullType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitArray(ArrayType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitArray(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitArray(ArrayType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitDeclared(DeclaredType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitDeclared(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitDeclared(DeclaredType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitError(ErrorType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitError(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitError(ErrorType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitTypeVariable(TypeVariable t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitTypeVariable(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitTypeVariable(TypeVariable t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitWildcard(WildcardType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitWildcard(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitWildcard(WildcardType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitExecutable(ExecutableType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitExecutable(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitExecutable(ExecutableType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitNoType(NoType t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitNoType(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitNoType(NoType t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }

        @Override
        public final T visitUnknown(TypeMirror t, StringBuilderAndState st) {
            try {
                st.depth++;
                return doVisitUnknown(t, st);
            } finally {
                st.depth--;
            }
        }

        protected T doVisitUnknown(TypeMirror t, StringBuilderAndState st) {
            return defaultAction(t, st);
        }
    }
}
