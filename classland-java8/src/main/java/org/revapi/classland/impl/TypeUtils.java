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

import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;

import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.PackageElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.mirror.ArrayTypeImpl;
import org.revapi.classland.impl.model.mirror.DeclaredTypeImpl;
import org.revapi.classland.impl.model.mirror.ExecutableTypeImpl;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
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

        @Override
        public PackageElementImpl visitTypeParameter(TypeParameterElement e, Void aVoid) {
            return visit(AS_TYPE_ELEMENT.visit(e.getBounds().get(0)));
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
        public Boolean visitIntersection(IntersectionType t, IntersectionType b) {
            return visitAll(t.getBounds(), b.getBounds());
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
                    && visitAll(t.getParameterTypes(), b.getParameterTypes())
                    && visitAll(t.getThrownTypes(), b.getThrownTypes());
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

    private static final TypeVisitor<TypeMirror, Universe> ERASER = new SimpleTypeVisitor8<TypeMirror, Universe>() {
        @Override
        public TypeMirror visitIntersection(IntersectionType t, Universe universe) {
            return visit(t.getBounds().get(0));
        }

        @Override
        protected TypeMirror defaultAction(TypeMirror e, Universe universe) {
            return e;
        }

        @Override
        public TypeMirror visitPrimitive(PrimitiveType t, Universe universe) {
            return t;
        }

        @Override
        public TypeMirror visitArray(ArrayType t, Universe universe) {
            return new ArrayTypeImpl((TypeMirrorImpl) visit(t.getComponentType()), -1, AnnotationSource.MEMOIZED_EMPTY,
                    AnnotationTargetPath.ROOT, obtainedNull());
        }

        @Override
        public TypeMirror visitDeclared(DeclaredType t, Universe universe) {
            return ((DeclaredTypeImpl) t).rebind((TypeMirrorImpl) visit(t.getEnclosingType()), emptyList());
        }

        @Override
        public TypeMirror visitError(ErrorType t, Universe universe) {
            return visitDeclared(t, null);
        }

        @Override
        public TypeMirror visitWildcard(WildcardType t, Universe universe) {
            if (t.getSuperBound() != null) {
                return visit(t.getSuperBound(), universe);
            } else if (t.getExtendsBound() != null) {
                return t.getExtendsBound();
            } else {
                return TypeMirrorFactory.createJavaLangObject(universe);
            }
        }

        @Override
        public TypeMirror visitExecutable(ExecutableType t, Universe universe) {
            TypeMirrorImpl ret = (TypeMirrorImpl) visit(t.getReturnType());
            List<TypeMirrorImpl> params = t.getParameterTypes().stream().map(p -> (TypeMirrorImpl) visit(p, universe))
                    .collect(toList());
            TypeMirrorImpl receiver = (TypeMirrorImpl) visit(t.getReceiverType());
            List<TypeMirrorImpl> thrown = t.getThrownTypes().stream().map(tt -> (TypeMirrorImpl) visit(tt, universe))
                    .collect(toList());

            return new ExecutableTypeImpl(universe, emptyList(), ret, receiver, params, thrown,
                    AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, obtainedNull());
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable t, Universe universe) {
            return visit(t.getUpperBound(), universe);
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
    static <T extends TypeMirror> T erasure(T type) {
        return (T) ERASER.visit(type);
    }

    static boolean isSubSignature(ExecutableType subSignature, ExecutableType signature) {
        return HAS_SAME_ARGS.visit(subSignature, signature) || HAS_SAME_ARGS.visit(subSignature, erasure(signature));
    }

    static TypeElement getNearestType(Element el) {
        while (el != null && !el.getKind().isClass() && !el.getKind().isInterface()) {
            el = el.getEnclosingElement();
        }

        return (TypeElement) el;
    }

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

    static boolean isMemberOf(ExecutableElement method, TypeElement type) {
        // TODO implement
        return false;
    }

    static boolean overrides(ExecutableElement overrider, ExecutableElement overridden, TypeElement type) {
        // TODO implement
        return false;
    }

    private static Boolean visitAllWith(TypeVisitor<Boolean, TypeMirror> visitor, List<? extends TypeMirror> as,
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
}
