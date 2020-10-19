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

import static org.revapi.classland.impl.model.AnnotatedConstructImpl.parseAnnotations;
import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;
import static org.revapi.classland.impl.util.MemoizedValue.obtainedNull;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.SimpleElementVisitor8;

import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.AnnotatedConstructImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ElementImpl;
import org.revapi.classland.impl.model.element.MissingTypeImpl;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.model.element.TypeElementImpl;
import org.revapi.classland.impl.model.signature.Bound;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.model.signature.TypeVariableResolutionContext;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public final class TypeMirrorFactory {
    private TypeMirrorFactory() {

    }

    private static final TypeSignature.Visitor<TypeMirrorImpl, ResolutionContext> SIGNATURE_VISITOR = new TypeSignature.Visitor<TypeMirrorImpl, ResolutionContext>() {
        @Override
        public TypeMirrorImpl visitPrimitiveType(TypeSignature.PrimitiveType type, ResolutionContext ctx) {
            return type.type == TypeKind.VOID
                    ? new NoTypeImpl(ctx.universe,
                            memoize(() -> parseAnnotations(ctx.universe, ctx.annotationSource.get(), ctx.path,
                                    ctx.typeLookupSeed.get(), true)),
                            TypeKind.VOID)
                    : asArray(
                            new PrimitiveTypeImpl(ctx.universe, type.type, ctx.annotationSource,
                                    targetArrayDimension(ctx.path, type), ctx.typeLookupSeed),
                            type.arrayDimension, ctx);
        }

        @Override
        public TypeMirrorImpl visitTypeVariable(TypeSignature.Variable typeVariable, ResolutionContext ctx) {
            // TODO this is not correct... Type variables can also represent wildcard capture, which
            // is currently not covered here...
            return ctx.variables.resolveTypeVariable(typeVariable.name)
                    .map(tp -> new TypeVariableImpl(tp, ctx.annotationSource, ctx.path, ctx.typeLookupSeed))
                    .orElse(null);
        }

        @Override
        public TypeMirrorImpl visitType(TypeSignature.Reference typeReference, ResolutionContext ctx) {
            TypeElementBase t = ctx.universe.getTypeByInternalNameFromModule(typeReference.internalTypeName,
                    ctx.typeLookupSeed.get());

            List<TypeMirrorImpl> args = new ArrayList<>(typeReference.typeArguments.size());
            int i = 0;
            for (Bound b : typeReference.typeArguments) {
                AnnotationTargetPath oldPath = ctx.path;
                AnnotationTargetPath newPath = ctx.path.clone().typeArgument(i++);
                ctx.path = newPath;
                switch (b.boundType) {
                case UNBOUNDED:
                    args.add(new WildcardTypeImpl(ctx.universe, null, null, ctx.annotationSource, ctx.path,
                            ctx.typeLookupSeed));
                    break;
                case EXACT:
                    args.add(b.type.accept(this, ctx));
                    break;
                case SUPER:
                    newPath.wildcardBound();
                    args.add(new WildcardTypeImpl(ctx.universe, null, b.type.accept(this, ctx), ctx.annotationSource,
                            ctx.path, ctx.typeLookupSeed));
                    break;
                case EXTENDS:
                    newPath.wildcardBound();
                    args.add(new WildcardTypeImpl(ctx.universe, b.type.accept(this, ctx), null, ctx.annotationSource,
                            ctx.path, ctx.typeLookupSeed));
                    break;
                default:
                    throw new IllegalStateException("Unhandled bound " + b);
                }
                ctx.path = oldPath;
            }

            TypeMirrorImpl enclosing = typeReference.outerClass == null ? null
                    : typeReference.outerClass.accept(this, ctx);

            TypeMirrorImpl ret;
            if (t instanceof MissingTypeImpl) {
                ret = new ErrorTypeImpl(ctx.universe, t, enclosing, args, ctx.annotationSource,
                        targetArrayDimension(ctx.path, typeReference));
            } else {
                ret = new DeclaredTypeImpl(ctx.universe, t, enclosing, args, ctx.annotationSource,
                        targetArrayDimension(ctx.path, typeReference));
            }

            return asArray(ret, typeReference.arrayDimension, ctx);
        }

        private AnnotationTargetPath targetArrayDimension(AnnotationTargetPath path, TypeSignature.Arrayable type) {
            AnnotationTargetPath ret = path.clone();
            int dim = type.arrayDimension;
            while (dim-- > 0) {
                ret = ret.array();
            }

            return ret;
        }

        private TypeMirrorImpl asArray(TypeMirrorImpl type, int dimensions, ResolutionContext ctx) {
            int dim = 0;
            while (dim++ < dimensions) {
                type = new ArrayTypeImpl(type, dim, ctx.annotationSource, ctx.path, ctx.typeLookupSeed);
            }

            return type;
        }
    };

    private static final ElementVisitor<TypeMirrorImpl, Void> MIRROR_OF_TYPE = new SimpleElementVisitor8<TypeMirrorImpl, Void>(
            null) {
        @Override
        public TypeMirrorImpl visitType(TypeElement e, Void __) {
            return (TypeMirrorImpl) e.asType();
        }
    };

    public static DeclaredTypeImpl create(Universe universe, TypeElementImpl element) {
        return new DeclaredTypeImpl(universe, element, MIRROR_OF_TYPE.visit(element.getEnclosingElement()),
                element.getTypeParameters().stream().map(TypeVariableImpl::new).collect(toList()),
                memoize(element::getAnnotationMirrors));
    }

    public static DeclaredTypeImpl create(Universe universe, TypeElementImpl element,
            List<TypeMirrorImpl> typeArguments, List<AnnotationMirrorImpl> annos) {
        return new DeclaredTypeImpl(universe, element, MIRROR_OF_TYPE.visit(element.getEnclosingElement()),
                typeArguments, obtained(annos));
    }

    public static DeclaredTypeImpl createJavaLangObject(Universe universe) {
        return (DeclaredTypeImpl) create(universe, Universe.JAVA_LANG_OBJECT_SIG, universe.noTypeVariables,
                AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT, obtainedNull());
    }

    public static TypeMirrorImpl create(Universe universe, TypeSignature type,
            TypeVariableResolutionContext resolutionContext, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath startPath, MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        return create(type,
                new ResolutionContext(universe, resolutionContext, annotationSource, startPath, typeLookupSeed));
    }

    public static TypeMirrorImpl create(Universe universe, TypeSignature type,
            TypeVariableResolutionContext resolutionContext, AnnotationTargetPath startPath) {
        return create(type, new ResolutionContext(universe, resolutionContext, resolutionContext.asAnnotationSource(),
                startPath, resolutionContext.lookupModule()));
    }

    public static PrimitiveTypeImpl createPrimitive(Universe universe, TypeKind kind) {
        return new PrimitiveTypeImpl(universe, kind, AnnotationSource.MEMOIZED_EMPTY, AnnotationTargetPath.ROOT,
                obtainedNull());
    }

    private static TypeMirrorImpl create(TypeSignature type, ResolutionContext ctx) {
        return type.accept(SIGNATURE_VISITOR, ctx);
    }

    private static final class ResolutionContext {
        final Universe universe;
        final TypeVariableResolutionContext variables;
        final MemoizedValue<AnnotationSource> annotationSource;
        final MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed;
        AnnotationTargetPath path;

        ResolutionContext(Universe universe, TypeVariableResolutionContext variables,
                MemoizedValue<AnnotationSource> annotationSource, AnnotationTargetPath path,
                MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
            this.universe = universe;
            this.variables = variables;
            this.annotationSource = annotationSource;
            this.path = path;
            this.typeLookupSeed = typeLookupSeed;
        }
    }
}
