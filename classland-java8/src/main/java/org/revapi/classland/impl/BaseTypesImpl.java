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

    private final TypeLookup lookup;
    private final MemoizedValue<TypeElementBase> javaLangObject;
    private final MemoizedValue<TypeMirror> javaLangObjectType;
    private final MemoizedFunction<PrimitiveType, TypeElement> boxedClass;
    private final NoType noType;

    protected BaseTypesImpl(TypeLookup lookup) {
        this.lookup = lookup;
        this.javaLangObject = memoize(lookup::getJavaLangObject);
        this.javaLangObjectType = javaLangObject.map(Element::asType);
        this.noType = new NoTypeImpl(lookup, obtained(emptyList()), NONE);
        this.boxedClass = MemoizedFunction.memoize(p -> {
            ModuleElementImpl javaBase = lookup.getJavaBase();
            switch (p.getKind()) {
            case BOOLEAN:
                return lookup.getTypeByInternalNameFromModule("java/lang/Boolean", javaBase);
            case CHAR:
                return lookup.getTypeByInternalNameFromModule("java/lang/Character", javaBase);
            case VOID:
                return lookup.getTypeByInternalNameFromModule("java/lang/Void", javaBase);
            case BYTE:
                return lookup.getTypeByInternalNameFromModule("java/lang/Byte", javaBase);
            case INT:
                return lookup.getTypeByInternalNameFromModule("java/lang/Integer", javaBase);
            case DOUBLE:
                return lookup.getTypeByInternalNameFromModule("java/lang/Double", javaBase);
            case FLOAT:
                return lookup.getTypeByInternalNameFromModule("java/lang/Float", javaBase);
            case LONG:
                return lookup.getTypeByInternalNameFromModule("java/lang/Long", javaBase);
            case SHORT:
                return lookup.getTypeByInternalNameFromModule("java/lang/Short", javaBase);
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

        return TypeUtils.isSameType(t1, t2);
    }

    @Override
    public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
        return TypeUtils.isSubType(t1, t2, true);
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
        return TypeUtils.isSubSignature(m1, m2, lookup);
    }

    @Override
    public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
        List<TypeMirrorImpl> ret = TypeUtils.getInterfaces(t, lookup);
        TypeMirrorImpl superType = TypeUtils.getSuperType(t, lookup);
        if (superType != null && superType.getKind() != NONE) {
            ret.add(0, superType);
        }
        return ret;
    }

    @Override
    public TypeMirror erasure(TypeMirror t) {
        return TypeUtils.erasure((TypeMirrorImpl) t, lookup);
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
        return TypeUtils.capture(t, lookup);
    }

    @Override
    public PrimitiveType getPrimitiveType(TypeKind kind) {
        return TypeMirrorFactory.createPrimitive(lookup, kind);
    }

    @Override
    public NullType getNullType() {
        return lookup.nullType;
    }

    @Override
    public NoType getNoType(TypeKind kind) {
        if (kind != VOID && kind != NONE) {
            throw new IllegalArgumentException();
        }
        return new NoTypeImpl(lookup, memoize(Collections::emptyList), kind);
    }

    @Override
    public ArrayType getArrayType(TypeMirror componentType) {
        return new ArrayTypeImpl((TypeMirrorImpl) componentType, -1, AnnotationSource.MEMOIZED_EMPTY,
                AnnotationTargetPath.ROOT, obtainedNull());
    }

    @Override
    public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
        return new WildcardTypeImpl(lookup, (TypeMirrorImpl) extendsBound, (TypeMirrorImpl) superBound,
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
            return new PrimitiveTypeImpl(lookup, BOOLEAN);
        case "java/lang/Byte":
            return new PrimitiveTypeImpl(lookup, BYTE);
        case "java/lang/Character":
            return new PrimitiveTypeImpl(lookup, CHAR);
        case "java/lang/Short":
            return new PrimitiveTypeImpl(lookup, SHORT);
        case "java/lang/Integer":
            return new PrimitiveTypeImpl(lookup, INT);
        case "java/lang/Long":
            return new PrimitiveTypeImpl(lookup, LONG);
        case "java/lang/Float":
            return new PrimitiveTypeImpl(lookup, FLOAT);
        case "java/lang/Double":
            return new PrimitiveTypeImpl(lookup, DOUBLE);
        case "java/lang/Void":
            return new PrimitiveTypeImpl(lookup, VOID);
        default:
            return INVALID_PRIMITIVE_TYPE;
        }
    }

    static boolean canConvert(PrimitiveType from, PrimitiveType to) {
        Set<TypeKind> valids = VALID_PRIMITIVE_CONVERSIONS.get(from.getKind());
        return valids != null && valids.contains(to.getKind());
    }
}
