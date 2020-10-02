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
package org.revapi.classland.impl.model.signature;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.lang.model.type.TypeKind;

import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.util.Asm;
import org.revapi.classland.impl.util.Nullable;

public final class SignatureParser {
    private SignatureParser() {
    }

    public static GenericTypeParameters parseType(String signature, @Nullable TypeElementBase outerClass) {
        ClassDecl visitor = new ClassDecl();
        SignatureReader rdr = new SignatureReader(signature);
        rdr.accept(visitor);
        return visitor.get(outerClass);
    }

    public static GenericMethodParameters parseMethod(String signature, TypeElementImpl declaringClass) {
        MethodDecl visitor = new MethodDecl();
        SignatureReader rdr = new SignatureReader(signature);
        rdr.accept(visitor);
        return visitor.get(declaringClass);
    }

    public static TypeSignature parseTypeRef(String signature) {
        TypeSig visitor = new TypeSig();
        SignatureReader rdr = new SignatureReader(signature);
        rdr.acceptType(visitor);
        return visitor.get();
    }

    public static TypeSignature parseInternalName(String internalName) {
        return parseTypeRef(Type.getObjectType(internalName).getDescriptor());
    }

    private static abstract class Decl extends SignatureVisitor {
        LinkedHashMap<String, TypeParameterBound> typeParams;
        String currentTypeParameter;
        TypeSig classBound;
        List<TypeSig> interfaceBounds;

        Decl() {
            super(Asm.VERSION);
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            finishTypeParam();

            currentTypeParameter = name;
            classBound = null;
            interfaceBounds = null;
        }

        void finishTypeParam() {
            ensureTypeParams();
            if (currentTypeParameter == null) {
                return;
            }
            TypeSignature clsBnd = classBound == null ? null : classBound.get();
            List<TypeSignature> ifaces = interfaceBounds == null ? emptyList()
                    : interfaceBounds.stream().map(TypeSig::get).collect(toList());

            TypeParameterBound bound = new TypeParameterBound(
                    classBound == null ? Bound.Type.UNBOUNDED : Bound.Type.EXTENDS, clsBnd, ifaces);

            typeParams.put(currentTypeParameter, bound);

            currentTypeParameter = null;
        }

        @Override
        public SignatureVisitor visitClassBound() {
            classBound = new TypeSig();
            return classBound;
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            ensureInterfaceBounds();
            TypeSig ret = new TypeSig();
            interfaceBounds.add(ret);
            return ret;
        }

        void ensureTypeParams() {
            if (typeParams == null) {
                typeParams = new LinkedHashMap<>();
            }
        }

        void ensureInterfaceBounds() {
            if (interfaceBounds == null) {
                interfaceBounds = new ArrayList<>();
            }
        }
    }

    private static final class MethodDecl extends Decl {
        List<TypeSig> parameters;
        TypeSig returnType;
        List<TypeSig> exceptions;

        GenericMethodParameters get(TypeElementImpl declaringClass) {
            finishTypeParam();

            List<TypeSignature> params = parameters == null ? emptyList()
                    : parameters.stream().map(TypeSig::get).collect(toList());
            TypeSignature ret = returnType == null ? null : returnType.get();

            List<TypeSignature> exs = exceptions == null ? emptyList()
                    : exceptions.stream().map(TypeSig::get).collect(toList());

            return new GenericMethodParameters(typeParams, ret, params, exs, declaringClass);
        }

        @Override
        public SignatureVisitor visitParameterType() {
            finishTypeParam();
            ensureParameters();
            TypeSig ret = new TypeSig();
            parameters.add(ret);
            return ret;
        }

        @Override
        public SignatureVisitor visitReturnType() {
            finishTypeParam();
            returnType = new TypeSig();
            return returnType;
        }

        @Override
        public SignatureVisitor visitExceptionType() {
            ensureExceptions();
            TypeSig ret = new TypeSig();
            exceptions.add(ret);
            return ret;
        }

        private void ensureParameters() {
            if (parameters == null) {
                parameters = new ArrayList<>();
            }
        }

        private void ensureExceptions() {
            if (exceptions == null) {
                exceptions = new ArrayList<>();
            }
        }
    }

    private static final class ClassDecl extends Decl {
        public GenericTypeParameters get(@Nullable TypeElementBase outerClass) {
            finishTypeParam();
            TypeSignature superType = classBound == null ? null : classBound.get();
            List<TypeSignature> interfaces = interfaceBounds == null ? emptyList()
                    : interfaceBounds.stream().map(TypeSig::get).collect(toList());

            return new GenericTypeParameters(typeParams, superType, interfaces, outerClass);
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            finishTypeParam();
            classBound = new TypeSig();
            return classBound;
        }

        @Override
        public SignatureVisitor visitInterface() {
            return visitInterfaceBound();
        }
    }

    private static class TypeSig extends SignatureVisitor {
        private final Bound.Type boundType;
        private int dim;
        private char baseType = 0;
        private String typeVar;
        private String className;
        private List<TypeSig> args;
        private TypeSignature outerClass;

        public TypeSig() {
            this(null);
        }

        public TypeSignature get() {
            if (baseType != 0) {
                return getBaseType();
            } else if (typeVar != null) {
                return getTypeVar();
            } else if (className != null) {
                return getReference();
            }
            return null;
        }

        private TypeSig(Bound.Type boundType) {
            super(Asm.VERSION);
            this.boundType = boundType;
        }

        @Override
        public void visitBaseType(char descriptor) {
            this.baseType = descriptor;
        }

        @Override
        public void visitTypeVariable(String name) {
            this.typeVar = name;
        }

        @Override
        public SignatureVisitor visitArrayType() {
            dim++;
            return this;
        }

        @Override
        public void visitClassType(String name) {
            this.className = name;
        }

        @Override
        public void visitInnerClassType(String name) {
            outerClass = get();
            className = className + "$" + name;
            dim = 0;
            typeVar = null;
            baseType = 0;
            args = null;
        }

        @Override
        public void visitTypeArgument() {
            ensureArgs();
            args.add(new TypeSig(Bound.Type.UNBOUNDED));
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            ensureArgs();
            TypeSig ret = new TypeSig(Bound.Type.fromWildcardDescriptor(wildcard));
            args.add(ret);
            return ret;
        }

        private void ensureArgs() {
            if (args == null) {
                args = new ArrayList<>();
            }
        }

        private Bound getBound() {
            if (boundType == Bound.Type.UNBOUNDED) {
                return new Bound(boundType, null);
            } else {
                return new Bound(boundType, get());
            }
        }

        private TypeSignature getReference() {
            List<Bound> as = args == null ? emptyList() : args.stream().map(TypeSig::getBound).collect(toList());

            return new TypeSignature.Reference(dim, className, as, outerClass);
        }

        private TypeSignature getTypeVar() {
            return new TypeSignature.Variable(dim, typeVar);
        }

        private TypeSignature getBaseType() {
            TypeKind type = null;
            switch (baseType) {
            case 'V':
                type = TypeKind.VOID;
                break;
            case 'Z':
                type = TypeKind.BOOLEAN;
                break;
            case 'C':
                type = TypeKind.CHAR;
                break;
            case 'B':
                type = TypeKind.BYTE;
                break;
            case 'S':
                type = TypeKind.SHORT;
                break;
            case 'I':
                type = TypeKind.INT;
                break;
            case 'F':
                type = TypeKind.FLOAT;
                break;
            case 'J':
                type = TypeKind.LONG;
                break;
            case 'D':
                type = TypeKind.DOUBLE;
                break;
            }

            if (type == null) {
                throw new IllegalStateException("Unknown primitive type descriptor: " + baseType);
            }

            return new TypeSignature.PrimitiveType(dim, type);
        }
    }
}
