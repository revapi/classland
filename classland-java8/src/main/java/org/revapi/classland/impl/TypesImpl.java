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
package org.revapi.classland.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;
import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
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
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;

import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.model.mirror.ArrayTypeImpl;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.NoTypeImpl;
import org.revapi.classland.impl.model.mirror.NullTypeImpl;
import org.revapi.classland.impl.model.mirror.PrimitiveTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.mirror.WildcardTypeImpl;
import org.revapi.classland.impl.util.MemoizedFunction;
import org.revapi.classland.impl.util.MemoizedValue;

public class TypesImpl implements Types {
    private final Universe universe;
    private final MemoizedValue<TypeElementBase> javaLangObject;
    private final MemoizedFunction<PrimitiveType, TypeElement> boxedClass;
    private final NoType noType;

    private final TypeVisitor<TypeMirror, Void> getSuperType = new SimpleTypeVisitor8<TypeMirror, Void>() {
        @Override
        protected TypeMirror defaultAction(TypeMirror e, Void unused) {
            return noType;
        }

        @Override
        public TypeMirror visitDeclared(DeclaredType t, Void unused) {
            // TODO implement
            return super.visitDeclared(t, unused);
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable t, Void unused) {
            // TODO implement
            return super.visitTypeVariable(t, unused);
        }

        @Override
        public TypeMirror visitArray(ArrayType t, Void unused) {
            // TODO implement
            return super.visitArray(t, unused);
        }
    };

    private final TypeVisitor<Boolean, TypeMirror> isSameType = new SimpleTypeVisitor8<Boolean, TypeMirror>() {
        @Override
        public Boolean visitIntersection(IntersectionType a, TypeMirror b) {
            // in javactypes visitClassType() the same for intersection and union types
//            if (t.isCompound() && s.isCompound()) {
//                if (!visit(supertype(t), supertype(s)))
//                    return false;
//
//                Map<Symbol, Type> tMap = new HashMap<>();
//                for (Type ti : interfaces(t)) {
//                    if (tMap.containsKey(ti)) {
//                        throw new AssertionError("Malformed intersection");
//                    }
//                    tMap.put(ti.tsym, ti);
//                }
//                for (Type si : interfaces(s)) {
//                    if (!tMap.containsKey(si.tsym))
//                        return false;
//                    Type ti = tMap.remove(si.tsym);
//                    if (!visit(ti, si))
//                        return false;
//                }
//                return tMap.isEmpty();
//            }

            return super.visitIntersection(a, b);
        }

        @Override
        public Boolean visitUnion(UnionType a, TypeMirror b) {
            // TODO implement
            return super.visitUnion(a, b);
        }

        @Override
        protected Boolean defaultAction(TypeMirror a, TypeMirror b) {
            return a == b;
        }

        @Override
        public Boolean visitPrimitive(PrimitiveType a, TypeMirror b) {
            return a == b || a.getKind() == b.getKind();
        }

        @Override
        public Boolean visitArray(ArrayType a, TypeMirror b) {
            return b.getKind() == TypeKind.ARRAY
                    && containsTypeEquivalent(a.getComponentType(), ((ArrayType) b).getComponentType());
        }

        @Override
        public Boolean visitDeclared(DeclaredType a, TypeMirror b) {
            if (a == b) {
                return true;
            }

            if (b.getKind() == TypeKind.WILDCARD && ((WildcardType) b).getSuperBound() != null) {
                WildcardType wb = (WildcardType) b;
                return visit(a, getWildcardUpperBound(wb)) && visit(a, getWildcardLowerBound(wb));
            }


            // TODO implement

            return super.visitDeclared(a, b);
        }

        @Override
        public Boolean visitError(ErrorType a, TypeMirror b) {
            // TODO implement
            return super.visitError(a, b);
        }

        @Override
        public Boolean visitTypeVariable(TypeVariable a, TypeMirror b) {
            // TODO implement
            return super.visitTypeVariable(a, b);
        }

        @Override
        public Boolean visitWildcard(WildcardType a, TypeMirror b) {
            // TODO implement
            return super.visitWildcard(a, b);
        }

        @Override
        public Boolean visitExecutable(ExecutableType a, TypeMirror b) {
            // TODO implement
            return super.visitExecutable(a, b);
        }

        @Override
        public Boolean visitNoType(NoType a, TypeMirror b) {
            // TODO implement
            return super.visitNoType(a, b);
        }
    };

    private TypeVisitor<Boolean, TypeMirror> containsType = new SimpleTypeVisitor8<Boolean, TypeMirror>() {
        // TODO copy this from JavacTypes
    };

    public TypesImpl(Universe universe) {
        this.universe = universe;
        this.javaLangObject = memoize(universe::getJavaLangObject);
        this.noType = new NoTypeImpl(universe, obtained(emptyList()), TypeKind.NONE);
        this.boxedClass = MemoizedFunction.memoize(p -> {
            ModuleElementImpl javaBase = universe.getJavaBase();
            switch (p.getKind()) {
                case BOOLEAN:
                    return universe.getTypeByInternalNameFromModule("java/lang/Boolean", javaBase);
                case CHAR:
                    return universe.getTypeByInternalNameFromModule("java/lang/Character", javaBase);
                case VOID:
                    return universe.getTypeByInternalNameFromModule("java/lang/Void", javaBase);
                case BYTE:
                    return universe.getTypeByInternalNameFromModule("java/lang/Byte", javaBase);
                case INT:
                    return universe.getTypeByInternalNameFromModule("java/lang/Integer", javaBase);
                case DOUBLE:
                    return universe.getTypeByInternalNameFromModule("java/lang/Double", javaBase);
                case FLOAT:
                    return universe.getTypeByInternalNameFromModule("java/lang/Float", javaBase);
                case LONG:
                    return universe.getTypeByInternalNameFromModule("java/lang/Long", javaBase);
                case SHORT:
                    return universe.getTypeByInternalNameFromModule("java/lang/Short", javaBase);
                default:
                    throw new IllegalArgumentException("Not a primitive type: " + p);

            }
        });
    }

    @Override
    public Element asElement(TypeMirror t) {
        if (t instanceof DeclaredType) {
            return ((DeclaredType) t).asElement();
        } else if (t instanceof TypeVariable) {
            return ((TypeVariable) t).asElement();
        } else {
            return null;
        }
    }

    @Override
    public boolean isSameType(TypeMirror t1, TypeMirror t2) {
        if (t1.getKind() == TypeKind.WILDCARD || t2.getKind() == TypeKind.WILDCARD) {
            return false;
        }

        return isSameType.visit(t1, t2);
    }

    @Override
    public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
        // TODO implement
        return false;
    }

    @Override
    public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
        // TODO implement
        return false;
    }

    @Override
    public boolean contains(TypeMirror t1, TypeMirror t2) {
        // TODO implement
        return false;
    }

    @Override
    public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
        return TypeUtils.isSubSignature(m1, m2);
    }

    @Override
    public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
        // TODO implement
        return null;
    }

    @Override
    public TypeMirror erasure(TypeMirror t) {
        return TypeUtils.erasure(t);
    }

    @Override
    public TypeElement boxedClass(PrimitiveType p) {
        return boxedClass.apply(p);
    }

    @Override
    public PrimitiveType unboxedType(TypeMirror t) {
        if (t.getKind() != TypeKind.DECLARED) {
            throw new IllegalArgumentException(t.toString());
        }

        String internalName = ((DeclaredTypeImpl) t).asElement().getInternalName();
        switch (internalName) {
            case "java/lang/Boolean":
                return new PrimitiveTypeImpl(universe, TypeKind.BOOLEAN);
            case "java/lang/Byte":
                return new PrimitiveTypeImpl(universe, TypeKind.BYTE);
            case "java/lang/Character":
                return new PrimitiveTypeImpl(universe, TypeKind.CHAR);
            case "java/lang/Short":
                return new PrimitiveTypeImpl(universe, TypeKind.SHORT);
            case "java/lang/Integer":
                return new PrimitiveTypeImpl(universe, TypeKind.INT);
            case "java/lang/Long":
                return new PrimitiveTypeImpl(universe, TypeKind.LONG);
            case "java/lang/Float":
                return new PrimitiveTypeImpl(universe, TypeKind.FLOAT);
            case "java/lang/Double":
                return new PrimitiveTypeImpl(universe, TypeKind.DOUBLE);
            case "java/lang/Void":
                return new PrimitiveTypeImpl(universe, TypeKind.VOID);
            default:
                throw new IllegalArgumentException(t.toString());
        }
    }

    @Override
    public TypeMirror capture(TypeMirror t) {
        // TODO implement
        return null;
    }

    @Override
    public PrimitiveType getPrimitiveType(TypeKind kind) {
        return TypeMirrorFactory.createPrimitive(universe, kind);
    }

    @Override
    public NullType getNullType() {
        return new NullTypeImpl(universe);
    }

    @Override
    public NoType getNoType(TypeKind kind) {
        return new NoTypeImpl(universe, memoize(Collections::emptyList), kind);
    }

    @Override
    public ArrayType getArrayType(TypeMirror componentType) {
        return new ArrayTypeImpl((TypeMirrorImpl) componentType, -1, AnnotationSource.MEMOIZED_EMPTY,
                AnnotationTargetPath.ROOT, obtainedNull());
    }

    @Override
    public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
        return new WildcardTypeImpl(universe, (TypeMirrorImpl) extendsBound, (TypeMirrorImpl) superBound,
                AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, obtainedNull());
    }

    @Override
    public DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs) {
        return getDeclaredType(null, typeElem, typeArgs);
    }

    @Override
    public DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem, TypeMirror... typeArgs) {
        return ((TypeElementBase) typeElem).asType().rebind((DeclaredTypeImpl) containing,
                Stream.of(typeArgs).map(t -> (TypeMirrorImpl) t).collect(toList()));
    }

    @Override
    public TypeMirror asMemberOf(DeclaredType containing, Element element) {
        // TODO implement
        return null;
    }

    private boolean containsTypeEquivalent(TypeMirror a, TypeMirror b) {
        return isSameType(a, b) ||
                containsType.visit(a, b) && containsType.visit(b, a);
    }

    private TypeMirror getWildcardUpperBound(TypeMirror t) {
        if (t.getKind() == TypeKind.WILDCARD) {
            WildcardType w = (WildcardType) t;
            TypeMirror explicitBound = w.getSuperBound();
            if (explicitBound != null) {
                return explicitBound;
            } else {
                TypeMirror extendsBound = w.getExtendsBound();
                if (extendsBound == null) {
                    return javaLangObject.get().asType();
                } else {
                    return getWildcardUpperBound(extendsBound);
                }
            }
        } else {
            return t;
        }
    }

    private TypeMirror getWildcardLowerBound(TypeMirror t) {
        if (t.getKind() == TypeKind.WILDCARD) {
            WildcardType w = (WildcardType) t;
            TypeMirror explicitLowerBound = w.getExtendsBound();
            TypeMirror explicitUpperBound = w.getSuperBound();
            if (explicitLowerBound != null) {
                return new NullTypeImpl(universe);
            } else if (explicitUpperBound == null) {
                return javaLangObject.get().asType();
            } else {
                return getWildcardLowerBound(explicitUpperBound);
            }
        } else {
            return t;
        }
    }
}
