/*
 * Copyright 2020-2021 Lukas Krejci
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
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.BOOLEAN;
import static javax.lang.model.type.TypeKind.BYTE;
import static javax.lang.model.type.TypeKind.CHAR;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.DOUBLE;
import static javax.lang.model.type.TypeKind.EXECUTABLE;
import static javax.lang.model.type.TypeKind.FLOAT;
import static javax.lang.model.type.TypeKind.INT;
import static javax.lang.model.type.TypeKind.LONG;
import static javax.lang.model.type.TypeKind.NONE;
import static javax.lang.model.type.TypeKind.SHORT;
import static javax.lang.model.type.TypeKind.VOID;
import static javax.lang.model.type.TypeKind.WILDCARD;

import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;
import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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

abstract class BaseTypesImpl implements Types {
    private static final PrimitiveTypeImpl INVALID_PRIMITIVE_TYPE = new PrimitiveTypeImpl(null, NONE);

    private static final EnumMap<TypeKind, EnumSet<TypeKind>> VALID_PRIMITIVE_CONVERSIONS = new EnumMap<>(
            TypeKind.class);

    static {
        VALID_PRIMITIVE_CONVERSIONS.put(BYTE, EnumSet.of(BYTE, SHORT, INT, LONG, FLOAT, DOUBLE));
        VALID_PRIMITIVE_CONVERSIONS.put(CHAR, EnumSet.of(CHAR, INT, LONG, FLOAT, DOUBLE));
        VALID_PRIMITIVE_CONVERSIONS.put(SHORT, EnumSet.of(SHORT, INT, LONG, FLOAT, DOUBLE));
        VALID_PRIMITIVE_CONVERSIONS.put(INT, EnumSet.of(INT, LONG, FLOAT, DOUBLE));
        VALID_PRIMITIVE_CONVERSIONS.put(LONG, EnumSet.of(LONG, FLOAT, DOUBLE));
        VALID_PRIMITIVE_CONVERSIONS.put(FLOAT, EnumSet.of(FLOAT, DOUBLE));
        VALID_PRIMITIVE_CONVERSIONS.put(DOUBLE, EnumSet.of(DOUBLE));
    }

    private final Universe universe;
    private final MemoizedValue<TypeElementBase> javaLangObject;
    private final MemoizedValue<TypeMirror> javaLangObjectType;
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

    // why is this not just TypeUtils.IS_SAME_TYPE?
    private final TypeVisitor<Boolean, TypeMirror> isSameType = new SimpleTypeVisitor8<Boolean, TypeMirror>() {
        @Override
        public Boolean visitIntersection(IntersectionType a, TypeMirror b) {
            // TODO implement
            // in javactypes visitClassType() the same for intersection and union types
            // if (t.isCompound() && s.isCompound()) {
            // if (!visit(supertype(t), supertype(s)))
            // return false;
            //
            // Map<Symbol, Type> tMap = new HashMap<>();
            // for (Type ti : interfaces(t)) {
            // if (tMap.containsKey(ti)) {
            // throw new AssertionError("Malformed intersection");
            // }
            // tMap.put(ti.tsym, ti);
            // }
            // for (Type si : interfaces(s)) {
            // if (!tMap.containsKey(si.tsym))
            // return false;
            // Type ti = tMap.remove(si.tsym);
            // if (!visit(ti, si))
            // return false;
            // }
            // return tMap.isEmpty();
            // }

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
            if (a == b) {
                return true;
            }

            return b.getKind() == ARRAY
                    && containsTypeEquivalent(a.getComponentType(), ((ArrayType) b).getComponentType());
        }

        @Override
        public Boolean visitDeclared(DeclaredType a, TypeMirror b) {
            if (a == b) {
                return true;
            }

            if (b.getKind() == WILDCARD && ((WildcardType) b).getSuperBound() != null) {
                WildcardType wb = (WildcardType) b;
                return visit(a, getWildcardUpperBound(wb)) && visit(a, getWildcardLowerBound(wb));
            }

            // TODO implement

            return super.visitDeclared(a, b);
        }

        @Override
        public Boolean visitError(ErrorType a, TypeMirror b) {
            // not sure why, but this is what javac does...
            return true;
        }

        @Override
        public Boolean visitTypeVariable(TypeVariable a, TypeMirror b) {
            // TODO implement
            return super.visitTypeVariable(a, b);
        }

        @Override
        public Boolean visitWildcard(WildcardType a, TypeMirror b) {
            if (b.getKind() != WILDCARD) {
                return false;
            }

            WildcardType bb = (WildcardType) b;

            TypeMirror aExtends = a.getExtendsBound();
            TypeMirror aSuper = a.getSuperBound();
            TypeMirror bExtends = bb.getExtendsBound();
            TypeMirror bSuper = bb.getSuperBound();

            if (aExtends != null) {
                if (bExtends == null) {
                    bExtends = javaLangObjectType.get();
                }
                return visit(aExtends, bExtends);
            } else if (aSuper != null) {
                if (bSuper == null) {
                    bSuper = javaLangObjectType.get();
                }
                return visit(aSuper, bSuper);
            }

            return bExtends == null && bSuper == null;
        }

        @Override
        public Boolean visitExecutable(ExecutableType a, TypeMirror b) {
            if (b.getKind() != EXECUTABLE) {
                return false;
            }

            ExecutableType bb = (ExecutableType) b;

            // TODO do we need to do this specialization?
            // if (!a.getTypeVariables().isEmpty()) {
            // return TypeUtils.hasSameBounds(a.getTypeVariables(), ((ExecutableType) b).getTypeVariables())
            // && visit()
            // }
            return TypeUtils.hasSameArgs(a, bb) && visit(a.getReturnType(), bb.getReturnType());
        }

        @Override
        public Boolean visitNoType(NoType a, TypeMirror b) {
            return a.getKind() == b.getKind();
        }
    };

    private TypeVisitor<Boolean, TypeMirror> containsType = new SimpleTypeVisitor8<Boolean, TypeMirror>() {
        // TODO copy this from JavacTypes
    };

    private TypeVisitor<List<? extends TypeMirror>, Void> getImplementedInterfaces = new SimpleTypeVisitor8<List<? extends TypeMirror>, Void>(
            emptyList()) {
        @Override
        public List<? extends TypeMirror> visitDeclared(DeclaredType t, Void unused) {
            return ((TypeElementBase) t.asElement()).getInterfaces();
        }

        @Override
        public List<? extends TypeMirror> visitTypeVariable(TypeVariable t, Void unused) {
            TypeMirror bound = t.getUpperBound();
            TypeKind boundType = bound.getKind();

            switch (boundType) {
            case INTERSECTION:
            case UNION:
                return getImplementedInterfaces.visit(bound);
            default:
                TypeElementBase el = TypeUtils.asTypeElement(bound);
                if (el != null && el.getKind() == ElementKind.INTERFACE) {
                    return singletonList(bound);
                } else {
                    return emptyList();
                }
            }
        }
    };

    private TypeVisitor<Boolean, TypeMirror> isSubtype = new SimpleTypeVisitor8<Boolean, TypeMirror>(false) {
        // TODO implement

        @Override
        public Boolean visitIntersection(IntersectionType t, TypeMirror typeMirror) {

            return super.visitIntersection(t, typeMirror);
        }

        @Override
        public Boolean visitUnion(UnionType t, TypeMirror typeMirror) {
            return super.visitUnion(t, typeMirror);
        }

        @Override
        public Boolean visitPrimitive(PrimitiveType t, TypeMirror typeMirror) {
            return super.visitPrimitive(t, typeMirror);
        }

        @Override
        public Boolean visitArray(ArrayType t, TypeMirror typeMirror) {
            return super.visitArray(t, typeMirror);
        }

        @Override
        public Boolean visitDeclared(DeclaredType t, TypeMirror typeMirror) {
            return super.visitDeclared(t, typeMirror);
        }

        @Override
        public Boolean visitError(ErrorType t, TypeMirror typeMirror) {
            return super.visitError(t, typeMirror);
        }

        @Override
        public Boolean visitTypeVariable(TypeVariable t, TypeMirror typeMirror) {
            return super.visitTypeVariable(t, typeMirror);
        }

        @Override
        public Boolean visitWildcard(WildcardType t, TypeMirror typeMirror) {
            return super.visitWildcard(t, typeMirror);
        }

        @Override
        public Boolean visitExecutable(ExecutableType t, TypeMirror typeMirror) {
            return super.visitExecutable(t, typeMirror);
        }
    };

    private TypeVisitor<List<TypeMirror>, List<TypeMirror>> getInterfaces = new SimpleTypeVisitor8<List<TypeMirror>, List<TypeMirror>>() {
        @Override
        public List<TypeMirror> visitIntersection(IntersectionType t, List<TypeMirror> typeMirrors) {
            typeMirrors.addAll(t.getBounds());
            return typeMirrors;
        }

        @Override
        protected List<TypeMirror> defaultAction(TypeMirror e, List<TypeMirror> typeMirrors) {
            return typeMirrors;
        }

        @Override
        public List<TypeMirror> visitArray(ArrayType t, List<TypeMirror> typeMirrors) {
            // TODO implement
            return typeMirrors;
        }

        @Override
        public List<TypeMirror> visitDeclared(DeclaredType t, List<TypeMirror> typeMirrors) {
            // TODO implement
            return typeMirrors;
        }

        @Override
        public List<TypeMirror> visitError(ErrorType t, List<TypeMirror> typeMirrors) {
            // TODO implement
            return typeMirrors;
        }
    };

    protected BaseTypesImpl(Universe universe) {
        this.universe = universe;
        this.javaLangObject = memoize(universe::getJavaLangObject);
        this.javaLangObjectType = javaLangObject.map(Element::asType);
        this.noType = new NoTypeImpl(universe, obtained(emptyList()), NONE);
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
        } else if (t instanceof IntersectionType) {
            return asElement(((IntersectionType) t).getBounds().get(0));
        } else {
            return null;
        }
    }

    @Override
    public boolean isSameType(TypeMirror t1, TypeMirror t2) {
        if (t1.getKind() == WILDCARD || t2.getKind() == WILDCARD) {
            return false;
        }

        return TypeUtils.isSameType((TypeMirrorImpl) t1, (TypeMirrorImpl) t2);
    }

    @Override
    public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
        if (t1.equals(t2)) {
            return true;
        }

        if (t2 instanceof IntersectionType) {
            IntersectionType it2 = (IntersectionType) t2;
            for (TypeMirror b : it2.getBounds()) {
                if (!isSubtype(t1, b)) {
                    return false;
                }
            }
            return true;
        }

        // TODO implement

        return isSubtype.visit(t1, t2);
    }

    @Override
    public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
        if (t1.getKind().isPrimitive()) {
            // this is not as precise as javactypes, because the typemirror in javac carries with it also the value
            // of field with a const value and this value is used when checking for convertibility. Let's just consider
            // that a corner case we don't support...
            PrimitiveType unboxedT1 = (PrimitiveType) t1;
            PrimitiveType unboxedT2 = unboxedOrNotype(t2);
            return canConvert(unboxedT1, unboxedT2);
        }
        return isSubtype(t1, t2);
    }

    // not used in revapi
    @Override
    public boolean contains(TypeMirror t1, TypeMirror t2) {
        // TODO implement
        return false;
    }

    // not used in revapi
    @Override
    public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
        return TypeUtils.isSubSignature(m1, m2, universe);
    }

    @Override
    public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
        List<TypeMirror> ret = getInterfaces.visit(t, new ArrayList<>());
        TypeMirror superType = getSuperType.visit(t);
        if (superType != null && superType.getKind() != NONE) {
            ret.add(0, superType);
        }
        return ret;
    }

    @Override
    public TypeMirror erasure(TypeMirror t) {
        return TypeUtils.erasure(t, universe);
    }

    @Override
    public TypeElement boxedClass(PrimitiveType p) {
        return boxedClass.apply(p);
    }

    @Override
    public PrimitiveType unboxedType(TypeMirror t) {
        PrimitiveType ret = unboxedOrNotype(t);
        if (ret.getKind() == NONE) {
            throw new IllegalArgumentException(t.toString());
        }

        return ret;
    }

    // not used in revapi
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
        if (kind != VOID && kind != NONE) {
            throw new IllegalArgumentException();
        }
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
        return TypeUtils.asMemberOf(containing, element);
    }

    private boolean containsTypeEquivalent(TypeMirror a, TypeMirror b) {
        return isSameType(a, b) || containsType.visit(a, b) && containsType.visit(b, a);
    }

    private TypeMirror getWildcardUpperBound(TypeMirror t) {
        if (t.getKind() == WILDCARD) {
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
        if (t.getKind() == WILDCARD) {
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

    /**
     * <b>WARNING</b>: this returns an invalid primitive type with kind "NONE" if the provided type cannot be unboxed.
     * React accordingly on the callsite to prevent returning such invalid value to the API user.
     */
    private PrimitiveType unboxedOrNotype(TypeMirror t) {
        if (t.getKind().isPrimitive()) {
            return (PrimitiveType) t;
        }

        if (t.getKind() != DECLARED) {
            return INVALID_PRIMITIVE_TYPE;
        }

        String internalName = ((DeclaredTypeImpl) t).asElement().getInternalName();
        switch (internalName) {
        case "java/lang/Boolean":
            return new PrimitiveTypeImpl(universe, BOOLEAN);
        case "java/lang/Byte":
            return new PrimitiveTypeImpl(universe, BYTE);
        case "java/lang/Character":
            return new PrimitiveTypeImpl(universe, CHAR);
        case "java/lang/Short":
            return new PrimitiveTypeImpl(universe, SHORT);
        case "java/lang/Integer":
            return new PrimitiveTypeImpl(universe, INT);
        case "java/lang/Long":
            return new PrimitiveTypeImpl(universe, LONG);
        case "java/lang/Float":
            return new PrimitiveTypeImpl(universe, FLOAT);
        case "java/lang/Double":
            return new PrimitiveTypeImpl(universe, DOUBLE);
        case "java/lang/Void":
            return new PrimitiveTypeImpl(universe, VOID);
        default:
            return INVALID_PRIMITIVE_TYPE;
        }
    }

    static boolean canConvert(PrimitiveType from, PrimitiveType to) {
        Set<TypeKind> valids = VALID_PRIMITIVE_CONVERSIONS.get(from.getKind());
        return valids != null && valids.contains(to.getKind());
    }
}
