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

import static java.util.stream.Collectors.toList;

import java.util.List;

import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor8;

import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.MissingTypeImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.element.TypeParameterElementImpl;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;

public final class TypeMirrorFactory {
    private TypeMirrorFactory() {

    }

    private static final TypeSignature.Visitor<TypeMirrorImpl, ResolutionContext> SIGNATURE_VISITOR = new TypeSignature.Visitor<>() {
        @Override
        public TypeMirrorImpl visitPrimitiveType(TypeSignature.PrimitiveType type, ResolutionContext ctx) {
            return new PrimitiveTypeImpl(ctx.universe, type.type);
        }

        @Override
        public TypeMirrorImpl visitTypeVariable(TypeSignature.Variable typeVariable, ResolutionContext ctx) {
            // TODO this is probably not correct... Type variables can also represent wildcard capture, which
            // is currently not covered here...
            return ctx.context.resolveTypeVariable(typeVariable.name).map(ElementImpl::asType).orElse(null);
        }

        @Override
        public TypeMirrorImpl visitType(TypeSignature.Reference typeReference, ResolutionContext ctx) {
            TypeElementBase t = ctx.universe.getTypeByInternalName(typeReference.internalTypeName);

            List<TypeMirrorImpl> args = typeReference.typeArguments.stream().map(b -> {
                switch (b.boundType) {
                case UNBOUNDED:
                    return new WildcardTypeImpl(ctx.universe, null, null);
                case EXACT:
                    return b.type.accept(this, ctx);
                case SUPER:
                    return new WildcardTypeImpl(ctx.universe, null, b.type.accept(this, ctx));
                case EXTENDS:
                    return new WildcardTypeImpl(ctx.universe, b.type.accept(this, ctx), null);
                }

                throw new IllegalStateException("Unhandled bound: " + b);
            }).collect(toList());

            TypeMirrorImpl enclosing = typeReference.outerClass == null ? null
                    : typeReference.outerClass.accept(this, ctx);

            if (t instanceof MissingTypeImpl) {
                return new ErrorTypeImpl(ctx.universe, t, enclosing, args);
            } else {
                return new DeclaredTypeImpl(ctx.universe, t, enclosing, args);
            }
        }
    };

    private static final ElementVisitor<TypeMirrorImpl, Void> MIRROR_OF_TYPE = new SimpleElementVisitor8<>(null) {
        @Override
        public TypeMirrorImpl visitType(TypeElement e, Void __) {
            return (TypeMirrorImpl) e.asType();
        }
    };

    public static DeclaredTypeImpl create(Universe universe, TypeElementImpl element) {
        return new DeclaredTypeImpl(universe, element, MIRROR_OF_TYPE.visit(element.getEnclosingElement()),
                element.getTypeParameters().stream().map(TypeMirrorFactory::create).collect(toList()));
    }

    public static DeclaredTypeImpl createJavaLangObject(Universe universe) {
        return (DeclaredTypeImpl) create(universe, Universe.JAVA_LANG_OBJECT_SIG, TypeVariableResolutionContext.EMPTY);
    }

    public static TypeMirrorImpl create(Universe universe, TypeSignature type,
            TypeVariableResolutionContext resolutionContext) {
        return create(type, new ResolutionContext(universe, resolutionContext));
    }

    private static TypeMirrorImpl create(TypeSignature type, ResolutionContext ctx) {
        return type.accept(SIGNATURE_VISITOR, ctx);
    }

    public static TypeVariableImpl create(TypeParameterElementImpl element) {
        return new TypeVariableImpl(element);
    }

    public static TypeVariableImpl create(Universe universe, TypeSignature upperBound, TypeSignature lowerBound,
            TypeVariableResolutionContext resolutionContext) {
        ResolutionContext ctx = new ResolutionContext(universe, resolutionContext);
        TypeMirrorImpl u = upperBound == null ? null : create(upperBound, ctx);
        TypeMirrorImpl l = lowerBound == null ? null : create(lowerBound, ctx);
        return new TypeVariableImpl(universe, u, l);
    }

    private static final class ResolutionContext {
        final Universe universe;
        final TypeVariableResolutionContext context;

        ResolutionContext(Universe universe, TypeVariableResolutionContext context) {
            this.universe = universe;
            this.context = context;
        }
    }
}
