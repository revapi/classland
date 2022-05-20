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

import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;

import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.mirror.*;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;
import org.revapi.classland.impl.util.TypePairVisitor;

final class TypeUtils {
    private static final ElementVisitor<PackageElementImpl, Void> PACKAGE_BY_ELEMENT = new SimpleElementVisitor8<PackageElementImpl, Void>() {

        @Override
        protected PackageElementImpl defaultAction(Element e, Void ignored) {
            return visit(e.getEnclosingElement());
        }

        @Override
        public PackageElementImpl visitPackage(PackageElement e, Void ignored) {
            return (PackageElementImpl) e;
        }

        @Override
        public PackageElementImpl visitUnknown(Element e, Void aVoid) {
            return null;
        }
    };

    private static final TypeVisitor<TypeElementBase, Void> AS_TYPE_ELEMENT = new SimpleTypeVisitor8<TypeElementBase, Void>() {
        final SimpleElementVisitor8<TypeElementBase, Void> ifType = new SimpleElementVisitor8<TypeElementBase, Void>() {
            @Override
            public TypeElementBase visitType(TypeElement e, Void aVoid) {
                return (TypeElementBase) e;
            }
        };

        @Override
        public TypeElementBase visitDeclared(DeclaredType t, Void ignored) {
            return ifType.visit(t.asElement());
        }

        @Override
        public TypeElementBase visitError(ErrorType t, Void ignored) {
            return visitDeclared(t, null);
        }

        @Override
        public TypeElementBase visitTypeVariable(TypeVariable t, Void aVoid) {
            return visit(t.getUpperBound(), null);
        }
    };

    private static final TypeVisitor<Boolean, TypeMirror> IS_SAME_TYPE = new TypePairVisitor<Boolean>(false) {
        @Override
        protected Boolean unmatchedAction(TypeMirror a, TypeMirror b) {
            // TODO there are some situations where javac considers different types of mirrors the same
            return super.unmatchedAction(a, b);
        }

        @Override
        public Boolean visitIntersection(IntersectionType t, IntersectionType b) {
            return visitKeyed(this, t.getBounds(), b.getBounds(), TypeMirror::toString);
        }

        @Override
        public Boolean visitPrimitive(PrimitiveType t, PrimitiveType b) {
            return t.getKind() == b.getKind();
        }

        @Override
        public Boolean visitNull(NullType t, NullType b) {
            return true;
        }

        @Override
        public Boolean visitArray(ArrayType t, ArrayType b) {
            return visit(t.getComponentType(), b.getComponentType());
        }

        @Override
        public Boolean visitDeclared(DeclaredType t, DeclaredType b) {
            return visitAll(t.getTypeArguments(), b.getTypeArguments())
                    && AS_TYPE_ELEMENT.visit(t) == AS_TYPE_ELEMENT.visit(b);
        }

        @Override
        public Boolean visitError(ErrorType t, ErrorType b) {
            return visitDeclared(t, b);
        }

        @Override
        public Boolean visitTypeVariable(TypeVariable t, TypeVariable b) {
            return visit(t.getUpperBound(), b.getUpperBound()) && visit(t.getLowerBound(), b.getLowerBound());
        }

        @Override
        public Boolean visitWildcard(WildcardType t, WildcardType b) {
            return nullableVisit(t.getExtendsBound(), b.getExtendsBound())
                    && nullableVisit(t.getSuperBound(), b.getSuperBound());
        }

        @Override
        public Boolean visitExecutable(ExecutableType t, ExecutableType b) {
            return visit(t.getReturnType(), b.getReturnType()) && visit(t.getReceiverType(), b.getReceiverType())
                    && visitAll(t.getTypeVariables(), t.getTypeVariables())
                    && visitAll(t.getParameterTypes(), b.getParameterTypes());
            // thrown exceptions not taken into account
            // && visitAll(t.getThrownTypes(), b.getThrownTypes());
        }

        @Override
        public Boolean visitNoType(NoType t, NoType b) {
            return t.getKind() == b.getKind();
        }

        private <T extends TypeMirror> boolean nullableVisit(@Nullable T a, @Nullable T b) {
            if (a == null) {
                return b == null;
            } else {
                if (b == null) {
                    return false;
                } else {
                    return visit(a, b);
                }
            }
        }

        private Boolean visitAll(List<? extends TypeMirror> as, List<? extends TypeMirror> bs) {
            return visitAllWith(this, as, bs);
        }
    };

    private static final TypeVisitor<Boolean, ExecutableType> HAS_SAME_ARGS = new SimpleTypeVisitor8<Boolean, ExecutableType>() {
        @Override
        public Boolean visitExecutable(ExecutableType e, ExecutableType other) {
            return visitAllWith(IS_SAME_TYPE, e.getParameterTypes(), other.getParameterTypes());
        }
    };

    private static final TypeVisitor<TypeMirrorImpl, TypeLookup> ERASER = new SimpleTypeVisitor8<TypeMirrorImpl, TypeLookup>() {
        @Override
        public TypeMirrorImpl visitIntersection(IntersectionType t, TypeLookup ignored) {
            return visit(t.getBounds().get(0));
        }

        @Override
        protected TypeMirrorImpl defaultAction(TypeMirror e, TypeLookup ignored) {
            return (TypeMirrorImpl) e;
        }

        @Override
        public TypeMirrorImpl visitPrimitive(PrimitiveType t, TypeLookup ignored) {
            return (TypeMirrorImpl) t;
        }

        @Override
        public TypeMirrorImpl visitArray(ArrayType t, TypeLookup ignored) {
            return new ArrayTypeImpl(visit(t.getComponentType()), -1, AnnotationSource.MEMOIZED_EMPTY,
                    AnnotationTargetPath.ROOT, obtainedNull());
        }

        @Override
        public TypeMirrorImpl visitDeclared(DeclaredType t, TypeLookup ignored) {
            return ((DeclaredTypeImpl) t).rebind(visit(t.getEnclosingType()), emptyList());
        }

        @Override
        public TypeMirrorImpl visitError(ErrorType t, TypeLookup ignored) {
            return visitDeclared(t, null);
        }

        @Override
        public TypeMirrorImpl visitWildcard(WildcardType t, TypeLookup lookup) {
            if (t.getSuperBound() != null) {
                return visit(t.getSuperBound(), lookup);
            } else if (t.getExtendsBound() != null) {
                return (TypeMirrorImpl) t.getExtendsBound();
            } else {
                return TypeMirrorFactory.createJavaLangObject(lookup);
            }
        }

        @Override
        public TypeMirrorImpl visitExecutable(ExecutableType t, TypeLookup lookup) {
            TypeMirrorImpl ret = visit(t.getReturnType());
            List<TypeMirrorImpl> params = t.getParameterTypes().stream().map(p -> (TypeMirrorImpl) visit(p, lookup))
                    .collect(toList());
            TypeMirrorImpl receiver = visit(t.getReceiverType());
            List<TypeMirrorImpl> thrown = t.getThrownTypes().stream().map(tt -> (TypeMirrorImpl) visit(tt, lookup))
                    .collect(toList());

            return new ExecutableTypeImpl(lookup, emptyList(), ret, receiver, params, thrown,
                    AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, obtainedNull());
        }

        @Override
        public TypeMirrorImpl visitTypeVariable(TypeVariable t, TypeLookup lookup) {
            return visit(t.getUpperBound(), lookup);
        }
    };

    private static final TypeVisitor<TypeMirror, TypeLookup> CAPTURE = new SimpleTypeVisitor8<TypeMirror, TypeLookup>() {
        @Override
        protected TypeMirror defaultAction(TypeMirror e, TypeLookup ignored) {
            return e;
        }

        @Override
        public TypeMirror visitDeclared(DeclaredType t, TypeLookup lookup) {
            if (t.getEnclosingType() != null) {
                TypeMirror capturedEnclosing = visit(t.getEnclosingType(), lookup);

            }
            return super.visitDeclared(t, lookup);
        }
        // TODO implement
    };

    private static final TypeVisitor<TypeMirrorImpl, ElementImpl> AS_MEMBER = new SimpleTypeVisitor8<TypeMirrorImpl, ElementImpl>() {
        @Override
        public TypeMirrorImpl visitIntersection(IntersectionType t, ElementImpl element) {
            return new IntersectionTypeImpl(element.getLookup(),
                    t.getBounds().stream().map(b -> visit(b, element)).collect(Collectors.toList()));
        }

        @Override
        protected TypeMirrorImpl defaultAction(TypeMirror e, ElementImpl element) {
            return element.asType();
        }

        @Override
        public TypeMirrorImpl visitArray(ArrayType t, ElementImpl element) {
            ArrayTypeImpl tt = (ArrayTypeImpl) t;
            return tt.withComponentType(visit(t.getComponentType(), element));
        }

        @Override
        public TypeMirrorImpl visitDeclared(DeclaredType t, ElementImpl element) {
            TypeElementBase owner = getNearestEnclosingType(element);

            List<TypeMirrorImpl> ownerParams = getAllTypeParameters(owner.asType());

            if (ownerParams.isEmpty()) {
                return element.asType();
            }

            TypeMirror base = asEnclosingSuperTypeOf(t, owner);

            if (t.getKind() == TypeKind.WILDCARD) {
                base = capture(base, element.getLookup());
            }

            if (!(base instanceof DeclaredTypeImpl)) {
                return element.asType();
            }

            List<TypeMirrorImpl> allBaseParams = getAllTypeParameters((DeclaredTypeImpl) base);
            List<TypeMirrorImpl> allOwnerParams = getAllTypeParameters(owner.asType());

            if (!allOwnerParams.isEmpty()) {
                if (allBaseParams.isEmpty()) {
                    return erasure(element.asType(), element.getLookup());
                } else {
                    return substitute(element.asType(), allOwnerParams, allBaseParams);
                }
            }

            return element.asType();
        }

        @Override
        public TypeMirrorImpl visitError(ErrorType t, ElementImpl element) {
            return visitDeclared(t, element);
        }

        @Override
        public TypeMirrorImpl visitTypeVariable(TypeVariable t, ElementImpl element) {
            return visit(t.getUpperBound(), element);
        }

        @Override
        public TypeMirrorImpl visitWildcard(WildcardType t, ElementImpl element) {
            return visit(WILDCARD_UPPER_BOUND.visit(t, element.getLookup()), element);
        }
    };

    private static final TypeVisitor<TypeMirrorImpl, TypeLookup> WILDCARD_UPPER_BOUND = new SimpleTypeVisitor8<TypeMirrorImpl, TypeLookup>() {
        @Override
        protected TypeMirrorImpl defaultAction(TypeMirror e, TypeLookup ignored) {
            return (TypeMirrorImpl) e;
        }

        @Override
        public TypeMirrorImpl visitWildcard(WildcardType t, TypeLookup lookup) {
            TypeMirror upper = t.getExtendsBound();
            return upper == null ? lookup.getJavaLangObject().asType() : visit(upper, lookup);
        }
    };

    private static final TypeVisitor<TypeMirror, TypeLookup> WILDCARD_LOWER_BOUND = new SimpleTypeVisitor8<TypeMirror, TypeLookup>() {
        @Override
        protected TypeMirror defaultAction(TypeMirror e, TypeLookup ignored) {
            return e;
        }

        @Override
        public TypeMirror visitWildcard(WildcardType t, TypeLookup lookup) {
            TypeMirror upper = t.getExtendsBound();
            return upper != null ? lookup.nullType : visit(t.getSuperBound());
        }
    };

    private static final TypeVisitor<TypeMirrorImpl, ElementImpl> AS_SUPER_TYPE_OF = new SimpleTypeVisitor8<TypeMirrorImpl, ElementImpl>() {
        @Override
        protected TypeMirrorImpl defaultAction(TypeMirror e, ElementImpl element) {
            return null;
        }

        @Override
        public TypeMirrorImpl visitArray(ArrayType t, ElementImpl element) {
            return isSubType(t, element.asType(), true) ? element.asType() : null;
        }

        @Override
        public TypeMirrorImpl visitDeclared(DeclaredType t, ElementImpl element) {
            if (t.asElement().equals(element)) {
                return (TypeMirrorImpl) t;
            }

            TypeMirror superType = GET_SUPER_TYPE.visit(t, element.getLookup());
            TypeKind superTypeKind = superType.getKind();

            if (superTypeKind == TypeKind.DECLARED || superTypeKind == TypeKind.TYPEVAR) {
                TypeMirrorImpl cast = visit(superType, element);
                if (cast != null) {
                    return cast;
                }
            }

            if (element.getKind() == ElementKind.INTERFACE) {
                for (TypeMirror iface : GET_INTERFACES.visit(t)) {
                    if (iface.getKind() == TypeKind.ERROR) {
                        continue;
                    }

                    TypeMirrorImpl cast = AS_SUPER_TYPE_OF.visit(iface, element);
                    if (cast != null) {
                        return cast;
                    }
                }
            }
            return null;
        }

        @Override
        public TypeMirrorImpl visitError(ErrorType t, ElementImpl element) {
            return (TypeMirrorImpl) t;
        }

        @Override
        public TypeMirrorImpl visitTypeVariable(TypeVariable t, ElementImpl element) {
            return visit(t.getUpperBound(), element);
        }
    };

    private static final TypeVisitor<TypeMirror, TypeLookup> GET_SUPER_TYPE = new SimpleTypeVisitor8<TypeMirror, TypeLookup>() {
        @Override
        protected TypeMirror defaultAction(TypeMirror e, TypeLookup lookup) {
            return new NoTypeImpl(lookup, MemoizedValue.obtainedEmptyList(), TypeKind.NONE);
        }

        @Override
        public TypeMirror visitArray(ArrayType t, TypeLookup lookup) {
            TypeMirror componentType = t.getComponentType();
            if (componentType.getKind().isPrimitive()
                    || isSameType(componentType, lookup.getJavaLangObject().asType())) {
                // JLS defines the super type of all arrays to implement Cloneable and Serializable
                return new IntersectionTypeImpl(lookup, Arrays.asList(
                        lookup.getTypeByInternalNameFromModule("java/io/Serializable", lookup.getJavaBase()).asType(),
                        lookup.getTypeByInternalNameFromModule("java/lang/Cloneable", lookup.getJavaBase()).asType()));
            } else {
                return ((ArrayTypeImpl) t).withComponentType((TypeMirrorImpl) visit(componentType));
            }
        }

        @Override
        public TypeMirror visitDeclared(DeclaredType t, TypeLookup lookup) {
            DeclaredTypeImpl tt = (DeclaredTypeImpl) t;
            TypeMirrorImpl superClass = tt.asElement().getSuperclass();

            ElementImpl sourceElement = ((DeclaredTypeImpl) t).getSource();
            if (sourceElement != null && sourceElement.getKind() == ElementKind.INTERFACE) {
                // super type of interface is java.lang.Object
                return lookup.getJavaLangObject().asType();
            } else {
                TypeMirrorImpl boundingClass = GET_BOUNDING_CLASS.visit(tt, lookup);
                List<TypeMirrorImpl> actualTypeParams = boundingClass instanceof DeclaredTypeImpl
                        ? getAllTypeParameters((DeclaredTypeImpl) boundingClass) : emptyList();
                DeclaredTypeImpl declaredType = tt.asElement().asType();

                List<TypeMirrorImpl> formalTypeParams = getAllTypeParameters(declaredType);

                if (isRawType(tt)) {
                    return erasure(superClass, lookup);
                } else if (!formalTypeParams.isEmpty()) {
                    return substitute(superClass, formalTypeParams, actualTypeParams);
                } else {
                    return superClass;
                }
            }
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable t, TypeLookup lookup) {
            TypeMirrorImpl upperBound = (TypeMirrorImpl) t.getUpperBound();
            TypeKind upperKind = upperBound.getKind();
            if (upperKind == TypeKind.TYPEVAR || (upperKind != TypeKind.INTERSECTION && upperBound.getSource() != null
                    && upperBound.getSource().getKind() != ElementKind.INTERFACE)) {
                return upperBound;
            } else {
                return visit(upperBound, lookup);
            }
        }
    };

    private static final IsSubType IS_SUB_TYPE = new IsSubType();

    private static final SimpleTypeVisitor8<List<TypeMirrorImpl>, TypeLookup> GET_INTERFACES = new SimpleTypeVisitor8<List<TypeMirrorImpl>, TypeLookup>() {
        @Override
        protected List<TypeMirrorImpl> defaultAction(TypeMirror e, TypeLookup unused) {
            return emptyList();
        }

        @Override
        public List<TypeMirrorImpl> visitDeclared(DeclaredType t, TypeLookup lookup) {
            DeclaredTypeImpl tt = (DeclaredTypeImpl) t;

            List<TypeMirrorImpl> interfaces = tt.asElement().getInterfaces();

            List<TypeMirrorImpl> actualParams = getAllTypeParameters(tt);
            List<TypeMirrorImpl> formalParams = getAllTypeParameters(tt.asElement().asType());

            if (isRawType(tt)) {
                return interfaces.stream().map(i -> erasure(i, lookup)).collect(toList());
            } else if (!formalParams.isEmpty()) {
                return interfaces.stream().map(i -> substitute(i, formalParams, actualParams)).collect(toList());
            } else {
                return interfaces;
            }
        }

        @Override
        public List<TypeMirrorImpl> visitTypeVariable(TypeVariable t, TypeLookup unused) {
            TypeVariableImpl tt = (TypeVariableImpl) t;
            TypeMirrorImpl bound = tt.getUpperBound();
            if (bound.getSource() != null && bound.getSource().getKind() == ElementKind.INTERFACE) {
                return singletonList(bound);
            }

            return emptyList();
        }
    };

    private static final SimpleTypeVisitor8<TypeMirrorImpl, TypeLookup> GET_BOUNDING_CLASS = new SimpleTypeVisitor8<TypeMirrorImpl, TypeLookup>() {
        @Override
        protected TypeMirrorImpl defaultAction(TypeMirror e, TypeLookup unused) {
            return (TypeMirrorImpl) e;
        }

        @Override
        public TypeMirrorImpl visitDeclared(DeclaredType t, TypeLookup lookup) {
            DeclaredTypeImpl tt = (DeclaredTypeImpl) t;
            TypeMirrorImpl outerBound = visit(t.getEnclosingType(), lookup);
            if (outerBound != t.getEnclosingType()) {
                return tt.rebind(outerBound, tt.getTypeArguments());
            } else {
                return tt;
            }
        }

        @Override
        public TypeMirrorImpl visitTypeVariable(TypeVariable t, TypeLookup lookup) {
            return visit(GET_SUPER_TYPE.visitTypeVariable(t, lookup), lookup);
        }
    };

    private static final SimpleTypeVisitor8<Boolean, TypeMirrorImpl> CONTAINS_TYPE = new SimpleTypeVisitor8<Boolean, TypeMirrorImpl>() {
        @Override
        public Boolean visitIntersection(IntersectionType t, TypeMirrorImpl contained) {
            return t.equals(contained) || t.getBounds().stream().anyMatch(b -> visit(b, contained));
        }

        @Override
        protected Boolean defaultAction(TypeMirror e, TypeMirrorImpl contained) {
            return e.equals(contained);
        }

        @Override
        public Boolean visitArray(ArrayType t, TypeMirrorImpl contained) {
            return t.equals(contained) || visit(t.getComponentType(), contained);
        }

        @Override
        public Boolean visitDeclared(DeclaredType t, TypeMirrorImpl contained) {
            return t.equals(contained) || visit(t.getEnclosingType(), contained)
                    || t.getTypeArguments().stream().anyMatch(a -> visit(a, contained));
        }

        @Override
        public Boolean visitWildcard(WildcardType t, TypeMirrorImpl contained) {
            return (t.getExtendsBound() != null && visit(t.getExtendsBound(), contained))
                    || (t.getSuperBound() != null && visit(t.getSuperBound(), contained));
        }

        @Override
        public Boolean visitExecutable(ExecutableType t, TypeMirrorImpl contained) {
            // interestingly, javac seems to ignore type vars here.
            // although not sure what exactly ForAll.qtype represents in javac.
            // is it the erased type
            return t.equals(contained) || visit(t.getReturnType(), contained)
                    || t.getParameterTypes().stream().anyMatch(p -> visit(p, contained))
                    || t.getThrownTypes().stream().anyMatch(e -> visit(e, contained));
        }
    };

    private TypeUtils() {
    }

    static @Nullable TypeElementBase asTypeElement(TypeMirror type) {
        return AS_TYPE_ELEMENT.visit(type);
    }

    static @Nullable PackageElementImpl getPackageOf(Element e) {
        return PACKAGE_BY_ELEMENT.visit(e);
    }

    @SuppressWarnings("unchecked")
    static <T extends TypeMirrorImpl> T erasure(T type, TypeLookup lookup) {
        return (T) ERASER.visit(type, lookup);
    }

    @SuppressWarnings("unchecked")
    static <T extends TypeMirror> T capture(T type, TypeLookup lookup) {
        return (T) CAPTURE.visit(type, lookup);
    }

    static boolean isSubSignature(ExecutableType subSignature, ExecutableType signature, TypeLookup lookup) {
        return HAS_SAME_ARGS.visit(subSignature, signature)
                || HAS_SAME_ARGS.visit(subSignature, erasure((ExecutableTypeImpl) signature, lookup));
    }

    static boolean hasSameArgs(ExecutableType subSignature, ExecutableType signature) {
        return HAS_SAME_ARGS.visit(subSignature, signature);
    }

    static TypeElementBase getNearestEnclosingType(ElementImpl el) {
        do {
            el = el.getEnclosingElement();
        } while (el != null && !el.getKind().isClass() && !el.getKind().isInterface());

        return (TypeElementBase) el;
    }

    static TypeElement getNearestType(Element el) {
        while (el != null && !el.getKind().isClass() && !el.getKind().isInterface()) {
            el = el.getEnclosingElement();
        }

        return (TypeElement) el;
    }

    /**
     * Checks whether {@code superClass} is a super class of the {@code subClass}. This only takes into account class
     * inheritance, not interfaces.
     *
     * @param subClass
     *            the sub-class
     * @param superClass
     *            the super-class
     * 
     * @return true if subClass is sub-class of the superClass, false otherwise
     */
    static boolean isSubclass(TypeElement subClass, TypeElement superClass) {
        while (subClass != null) {
            if (subClass == superClass) {
                return true;
            } else {
                subClass = asTypeElement(subClass.getSuperclass());
            }
        }

        return false;
    }

    /**
     * Similar to {@link #isSubclass(TypeElement, TypeElement)} but takes into account the whole inheritance hierarchy
     * including the implemented interfaces.
     *
     * @param subClass
     *            the sub-class
     * @param superClass
     *            the super-class
     * 
     * @return true if the subClass is a sub-type of the superClass, false otherwise
     */
    static boolean isSubType(TypeElement subClass, TypeElement superClass) {
        return isSubType(subClass.asType(), superClass.asType(), true);
    }

    static boolean isSubType(TypeMirror subType, TypeMirror superType, boolean capture) {
        if (subType.equals(superType)) {
            return true;
        }

        TypeMirrorImpl ssuperType = (TypeMirrorImpl) superType;

        if (superType.getKind() == TypeKind.INTERSECTION) {
            for (TypeMirror i : GET_INTERFACES.visit(superType, ssuperType.getLookup())) {
                if (!isSubType(subType, i, capture)) {
                    return false;
                }
            }

            return true;
        }

        if (capture) {
            subType = capture(subType, ssuperType.getLookup());
        }

        return IS_SUB_TYPE.visit(subType, ssuperType);
    }

    /**
     * This assumes that element is part of the target type or some of its supertypes.
     */
    static boolean isAccessibleIn(Element element, TypeElement target) {
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.PUBLIC)) {
            return true;
        } else if (modifiers.contains(Modifier.PRIVATE)) {
            return getNearestType(element) == target;
        } else if (modifiers.contains(Modifier.PROTECTED)) {
            return !target.getKind().isInterface();
        } else {
            // package private
            return getPackageOf(element) == getPackageOf(target);
        }
    }

    static boolean isConstructor(ExecutableElement m) {
        return m.getSimpleName().contentEquals("<init>");
    }

    static boolean isMemberOf(ExecutableElement method, TypeElement type) {
        TypeElement owner = getNearestType(method.getEnclosingElement());

        if (owner.equals(type)) {
            return true;
        }

        return isSubType(type, owner) && isAccessibleIn(method, type) && !isHiddenIn(method, type);
    }

    static TypeMirror asMemberOf(DeclaredType owner, Element decl) {
        // TODO: add check for owner being a subclass of decl's enclosing type
        if (decl.getModifiers().contains(Modifier.STATIC)) {
            return decl.asType();
        }

        return AS_MEMBER.visit(owner, (ElementImpl) decl);
    }

    static List<TypeMirrorImpl> getAllTypeParameters(DeclaredTypeImpl type) {
        List<TypeMirrorImpl> ret = new ArrayList<>();
        addAllTypeParameters(type, ret);
        return ret;
    }

    static void addAllTypeParameters(DeclaredTypeImpl type, List<TypeMirrorImpl> typeParameters) {
        TypeMirrorImpl enclosing = type.getEnclosingType();
        if (enclosing instanceof DeclaredTypeImpl) {
            addAllTypeParameters((DeclaredTypeImpl) enclosing, typeParameters);
        }
        typeParameters.addAll(type.getTypeArguments());
    }

    static TypeMirror asEnclosingSuperTypeOf(TypeMirror type, TypeElement superType) {
        TypeKind typeKind = type.getKind();
        boolean isClass = typeKind == TypeKind.DECLARED || typeKind == TypeKind.INTERSECTION;

        TypeElementBase ssuperType = (TypeElementBase) superType;
        if (isClass) {
            do {
                TypeMirror t = asSuperTypeOf(type, ssuperType);
                if (t != null) {
                    return t;
                }

                if (typeKind == TypeKind.DECLARED) {
                    type = ((DeclaredType) type).getEnclosingType();
                }

                if (type != null) {
                    typeKind = type.getKind();
                    isClass = typeKind == TypeKind.DECLARED || typeKind == TypeKind.INTERSECTION;
                } else {
                    isClass = false;
                }
            } while (isClass);
            return null;
        } else if (typeKind == TypeKind.ARRAY) {
            return isSubType(asTypeElement(type), superType) ? superType.asType() : null;
        } else if (typeKind == TypeKind.TYPEVAR) {
            return asSuperTypeOf(type, ssuperType);
        } else if (typeKind == TypeKind.ERROR) {
            return type;
        } else {
            return null;
        }
    }

    static TypeMirrorImpl asSuperTypeOf(TypeMirror type, ElementImpl superType) {
        return AS_SUPER_TYPE_OF.visit(type, superType);
    }

    static boolean isHiddenIn(Element element, TypeElement owner) {
        // TODO implement
        return false;
    }

    static boolean hasSameBounds(List<? extends TypeVariable> as, List<? extends TypeVariable> bs) {
        int len = as.size();
        if (bs.size() != len) {
            return false;
        }

        for (int i = 0; i < len; ++i) {
            TypeVariable a = as.get(i);
            TypeVariable b = bs.get(i);

            if (!IS_SAME_TYPE.visit(a, b)) {
                return false;
            }
        }

        return true;
    }

    static boolean isSameType(TypeMirror t1, TypeMirror t2) {
        return IS_SAME_TYPE.visit(t1, t2);
    }

    private static TypeMirrorImpl substitute(TypeMirrorImpl type, List<? extends TypeMirrorImpl> fromTypeParameters,
            List<? extends TypeMirrorImpl> toTypeParameters) {
        int maxIdx = Math.min(fromTypeParameters.size(), toTypeParameters.size());

        RecursiveTypeVisitor<Void> subst = new RecursiveTypeVisitor<Void>() {
            @Override
            public TypeMirrorImpl visitWildcard(WildcardType t, Void unused) {
                WildcardTypeImpl t2 = (WildcardTypeImpl) super.visitWildcard(t, null);
                if (t2 != t && ((WildcardTypeImpl) t).isExtends() && t2.isExtends()) {
                    return t2.rebind(WILDCARD_UPPER_BOUND.visit(t2), t2.getSuperBound());
                }
                return t2;
            }

            @Override
            public TypeMirrorImpl visitTypeVariable(TypeVariable t, Void unused) {
                for (int i = 0; i < maxIdx; ++i) {
                    TypeMirrorImpl from = fromTypeParameters.get(i);
                    TypeMirrorImpl to = toTypeParameters.get(i);

                    if (t.equals(from)) {
                        return to;
                    }
                }
                return (TypeMirrorImpl) t;
            }

            @Override
            public TypeMirrorImpl visitExecutable(ExecutableType t, Void unused) {
                List<TypeVariableImpl> substVars = substBounds(((ExecutableTypeImpl) t).getTypeVariables());
                TypeMirrorImpl newReturnType = visit(t.getReturnType());
                List<TypeMirrorImpl> newParams = t.getParameterTypes().stream().map(this::visit).collect(toList());
                List<TypeMirrorImpl> newThrown = t.getThrownTypes().stream().map(this::visit).collect(toList());

                if (substVars == t.getTypeVariables() && newReturnType == t.getReturnType()
                        && newParams.equals(t.getParameterTypes()) && newThrown.equals(t.getThrownTypes())) {
                    return (TypeMirrorImpl) t;
                } else {
                    return ((ExecutableTypeImpl) t).rebind(substVars, newReturnType,
                            ((ExecutableTypeImpl) t).getReceiverType(), newParams, newThrown);
                }
            }

            private List<TypeVariableImpl> substBounds(List<TypeVariableImpl> vars) {
                if (vars.isEmpty()) {
                    return vars;
                }

                List<TypeVariableImpl> newVars = vars.stream().map(v -> v.rebind(v.getLookup().nullType, null))
                        .collect(toList());

                boolean changed = false;
                List<TypeMirrorImpl> bounds = new ArrayList<>(vars.size());
                for (TypeVariableImpl v : vars) {
                    TypeMirrorImpl bound = substitute(v.getUpperBound(), vars, newVars);
                    bounds.add(bound);
                    if (!bound.equals(v.getUpperBound())) {
                        changed = true;
                    }
                }
                if (!changed) {
                    return vars;
                }

                for (int i = 0; i < newVars.size(); ++i) {
                    newVars.get(i).getUpperBoundValue().swap(bounds.get(i));
                }

                return newVars;
            }
        };

        return subst.visit(type);
    }

    private static boolean visitAllWith(TypeVisitor<Boolean, TypeMirror> visitor, List<? extends TypeMirror> as,
            List<? extends TypeMirror> bs) {
        if (as.size() != bs.size()) {
            return false;
        }

        for (int i = 0; i < as.size(); ++i) {
            TypeMirror a = as.get(i);
            TypeMirror b = bs.get(i);

            if (!visitor.visit(a, b)) {
                return false;
            }
        }

        return true;
    }

    private static <K> boolean visitKeyed(TypeVisitor<Boolean, TypeMirror> visitor, List<? extends TypeMirror> as,
            List<? extends TypeMirror> bs, Function<TypeMirror, K> keyExtractor) {
        if (as.size() != bs.size()) {
            return false;
        }

        Map<K, TypeMirror> mappedAs = new HashMap<>(as.size());
        for (TypeMirror t : as) {
            mappedAs.put(keyExtractor.apply(t), t);
        }

        for (TypeMirror b : bs) {
            TypeMirror a = mappedAs.remove(keyExtractor.apply(b));
            if (a == null) {
                return false;
            }

            if (!visitor.visit(a, b)) {
                return false;
            }
        }

        return true;
    }

    enum ElementNamespace {
        FIELDS, METHODS, TYPES, PACKAGES, MODULES, OTHER;

        static ElementNamespace of(ElementKind kind) {
            if (kind.isClass() || kind.isInterface()) {
                return TYPES;
            } else if (kind.isField()) {
                return FIELDS;
            } else if (kind.ordinal() == 17) {
                // we need to keep this java 8 compat, but also support later features.
                // let's go the dangerous way and rely on the enum constant order to
                // figure out what kind this is
                return MODULES;
            } else if (kind == ElementKind.PACKAGE) {
                return PACKAGES;
            } else if (kind == ElementKind.METHOD) {
                return METHODS;
            } else {
                return OTHER;
            }
        }
    }

    static boolean isRawType(DeclaredTypeImpl type) {
        List<TypeMirrorImpl> params = getAllTypeParameters(type);
        List<TypeMirrorImpl> declaredParams = getAllTypeParameters(type.asElement().asType());

        return !declaredParams.isEmpty() && params.isEmpty();
    }

    private static final class IsSubType extends SimpleTypeVisitor8<Boolean, TypeMirrorImpl> {
        private IsSubType() {
        }

        @Override
        public Boolean visitPrimitive(PrimitiveType t, TypeMirrorImpl typeMirror) {
            TypeKind a = t.getKind();
            TypeKind b = typeMirror.getKind();
            switch (a) {
            case VOID:
                return b == TypeKind.VOID;
            case BOOLEAN:
                return b == TypeKind.BOOLEAN;
            case BYTE:
                return b == TypeKind.BYTE || b == TypeKind.SHORT || b == TypeKind.INT || b == TypeKind.LONG
                        || b == TypeKind.FLOAT || b == TypeKind.DOUBLE;
            case CHAR:
                return b == TypeKind.CHAR || b == TypeKind.INT || b == TypeKind.LONG || b == TypeKind.FLOAT
                        || b == TypeKind.DOUBLE;
            case SHORT:
                return b == TypeKind.SHORT || b == TypeKind.INT || b == TypeKind.LONG || b == TypeKind.FLOAT
                        || b == TypeKind.DOUBLE;
            case INT:
                return b == TypeKind.INT || b == TypeKind.LONG || b == TypeKind.FLOAT || b == TypeKind.DOUBLE;
            case LONG:
                return b == TypeKind.LONG || b == TypeKind.FLOAT || b == TypeKind.DOUBLE;
            case FLOAT:
                return b == TypeKind.FLOAT || b == TypeKind.DOUBLE;
            default:
                return a == b;
            }
        }

        @Override
        public Boolean visitIntersection(IntersectionType t, TypeMirrorImpl typeMirror) {
            // TODO implement
            return super.visitIntersection(t, typeMirror);
        }

        @Override
        protected Boolean defaultAction(TypeMirror e, TypeMirrorImpl typeMirror) {
            return false;
        }

        @Override
        public Boolean visitArray(ArrayType t, TypeMirrorImpl typeMirror) {
            switch (typeMirror.getKind()) {
            case ARRAY:
                ArrayTypeImpl s = (ArrayTypeImpl) typeMirror;
                if (t.getComponentType().getKind().isPrimitive()) {
                    return isSameType(t.getComponentType(), s.getComponentType());
                } else {
                    return visit(t.getComponentType(), s.getComponentType());
                }
            case DECLARED:
                // arrays by spec are subclasses of Object and implement Cloneable and Serializable
                TypeElementBase el = ((DeclaredTypeImpl) typeMirror).asElement();
                TypeLookup lookup = typeMirror.getLookup();
                return el.equals(lookup.getJavaLangObject()) || el.equals(lookup.getJavaIoSerializable())
                        || el.equals(lookup.getJavaLangCloneable());
            default:
                return false;
            }
        }

        @Override
        public Boolean visitDeclared(DeclaredType t, TypeMirrorImpl typeMirror) {
            ElementImpl potentialSuperElement = typeMirror.getSource();
            if (potentialSuperElement == null) {
                return false;
            }
            TypeMirrorImpl superType = asSuperTypeOf(t, potentialSuperElement);
            if (superType == null) {
                return false;
            }
            if (superType.getKind() != TypeKind.DECLARED) {
                return isSubType(superType, typeMirror, false);
            }

            if (!potentialSuperElement.equals(superType.getSource())) {
                return false;
            }

            boolean isParameterized = typeMirror.getKind() == TypeKind.DECLARED
                    && !((DeclaredTypeImpl) typeMirror).getTypeArguments().isEmpty();

            if (isParameterized && !CONTAINS_TYPE.visit(typeMirror, superType)) {
                return false;
            }

            return isSubType(GET_ENCLOSING_TYPE.visit(superType), GET_ENCLOSING_TYPE.visit(typeMirror), false);
        }

        @Override
        public Boolean visitError(ErrorType t, TypeMirrorImpl typeMirror) {
            return true;
        }
    }

    private static final SimpleTypeVisitor8<TypeMirrorImpl, Void> GET_ENCLOSING_TYPE = new SimpleTypeVisitor8<TypeMirrorImpl, Void>() {
        @Override
        protected TypeMirrorImpl defaultAction(TypeMirror e, Void unused) {
            return null;
        }

        @Override
        public TypeMirrorImpl visitDeclared(DeclaredType t, Void unused) {
            return (TypeMirrorImpl) t.getEnclosingType();
        }

        @Override
        public TypeMirrorImpl visitError(ErrorType t, Void unused) {
            return new NoTypeImpl(((TypeMirrorImpl) t).getLookup(), MemoizedValue.obtainedEmptyList(), TypeKind.NONE);
        }
    };

    private static class RecursiveTypeVisitor<P> extends SimpleTypeVisitor8<TypeMirrorImpl, P> {
        @Override
        public TypeMirrorImpl visitIntersection(IntersectionType t, P p) {
            List<TypeMirrorImpl> converted = new ArrayList<>(t.getBounds().size());
            boolean changed = visitAll(t.getBounds(), converted, p);

            if (!changed) {
                return (TypeMirrorImpl) t;
            } else {
                return new IntersectionTypeImpl(((IntersectionTypeImpl) t).getLookup(), converted);
            }
        }

        @Override
        protected TypeMirrorImpl defaultAction(TypeMirror e, P p) {
            return (TypeMirrorImpl) e;
        }

        @Override
        public TypeMirrorImpl visitArray(ArrayType t, P p) {
            TypeMirrorImpl el = visit(t.getComponentType(), p);
            if (el.equals(t.getComponentType())) {
                return (TypeMirrorImpl) t;
            } else {
                return ((ArrayTypeImpl) t).withComponentType(el);
            }
        }

        @Override
        public TypeMirrorImpl visitDeclared(DeclaredType t, P p) {
            TypeMirrorImpl enclosing = visit(t.getEnclosingType(), p);

            boolean changed = enclosing.equals(t.getEnclosingType());

            List<TypeMirrorImpl> args = new ArrayList<>(t.getTypeArguments().size());
            changed = visitAll(t.getTypeArguments(), args, p) && changed;

            if (!changed) {
                return (TypeMirrorImpl) t;
            } else {
                return ((DeclaredTypeImpl) t).rebind(enclosing, args);
            }
        }

        @Override
        public TypeMirrorImpl visitWildcard(WildcardType t, P p) {
            TypeMirrorImpl extendsBound = null;
            TypeMirrorImpl superBound = null;
            if (t.getExtendsBound() != null) {
                extendsBound = visit(t.getExtendsBound(), p);
            }

            if (t.getSuperBound() != null) {
                superBound = visit(t.getSuperBound(), p);
            }

            if (Objects.equals(t.getExtendsBound(), extendsBound) && Objects.equals(t.getSuperBound(), superBound)) {
                return (TypeMirrorImpl) t;
            } else {
                return ((WildcardTypeImpl) t).rebind(extendsBound, superBound);
            }
        }

        @Override
        public TypeMirrorImpl visitTypeVariable(TypeVariable t, P p) {
            TypeMirrorImpl upper = visit(t.getUpperBound(), p);
            TypeMirrorImpl lower = visit(t.getLowerBound(), p);

            if (upper.equals(t.getUpperBound()) && lower.equals(t.getLowerBound())) {
                return (TypeMirrorImpl) t;
            } else {
                return ((TypeVariableImpl) t).rebind(lower, upper);
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public TypeMirrorImpl visitExecutable(ExecutableType t, P p) {
            TypeMirrorImpl ret = visit(t.getReturnType());

            boolean changed = ret.equals(t.getReturnType());

            List<TypeMirrorImpl> parameters = new ArrayList<>(t.getParameterTypes().size());
            List<TypeMirrorImpl> thrown = new ArrayList<>(t.getThrownTypes().size());
            List<TypeMirrorImpl> vars = new ArrayList<>(t.getTypeVariables().size());

            changed = visitAll(t.getParameterTypes(), parameters, p) && changed;
            changed = visitAll(t.getThrownTypes(), thrown, p) && changed;
            changed = visitAll(t.getTypeVariables(), vars, p) && changed;

            if (!changed) {
                return (TypeMirrorImpl) t;
            } else {
                return ((ExecutableTypeImpl) t).rebind((List<TypeVariableImpl>) (List) vars, ret,
                        ((ExecutableTypeImpl) t).getReceiverType(), parameters, thrown);
            }
        }

        private boolean visitAll(List<? extends TypeMirror> in, List<TypeMirrorImpl> out, P p) {
            boolean changed = false;
            for (TypeMirror i : in) {
                TypeMirrorImpl o = visit(i, p);
                out.add(o);
                if (!o.equals(i)) {
                    changed = true;
                }
            }

            return changed;
        }
    }
}
