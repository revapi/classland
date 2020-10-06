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
package org.revapi.classland.impl.model.element;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import static org.objectweb.asm.TypeReference.newFormalParameterReference;
import static org.revapi.classland.impl.util.MemoizedValue.memoize;
import static org.revapi.classland.impl.util.MemoizedValue.obtained;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.ParameterNode;
import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.mirror.TypeMirrorFactory;
import org.revapi.classland.impl.model.mirror.TypeMirrorImpl;
import org.revapi.classland.impl.model.signature.SignatureParser;
import org.revapi.classland.impl.model.signature.TypeSignature;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Modifiers;
import org.revapi.classland.impl.util.Nullable;

public abstract class VariableElementImpl extends ElementImpl implements VariableElement {
    private VariableElementImpl(Universe universe, MemoizedValue<AnnotationSource> annotationSource,
            MemoizedValue<@Nullable ModuleElementImpl> module) {
        super(universe, annotationSource, AnnotationTargetPath.ROOT, module);
    }

    private VariableElementImpl(Universe universe, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath path, MemoizedValue<@Nullable ModuleElementImpl> module) {
        super(universe, annotationSource, path, module);
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitVariable(this, p);
    }

    public static final class Missing extends VariableElementImpl {
        private final TypeElementBase parent;
        private final NameImpl name;
        private final ElementKind kind;
        private final TypeMirrorImpl type;

        public Missing(Universe universe, TypeElementBase parent, String name, String descriptor, ElementKind kind) {
            super(universe, AnnotationSource.MEMOIZED_EMPTY, parent.lookupModule());
            this.parent = parent;
            this.name = NameImpl.of(name);
            this.kind = kind;
            this.type = TypeMirrorFactory.create(universe, SignatureParser.parseTypeRef(descriptor), parent, AnnotationTargetPath.ROOT);
        }

        @Override
        public Object getConstantValue() {
            return null;
        }

        @Override
        public TypeMirrorImpl asType() {
            return type;
        }

        @Override
        public ElementKind getKind() {
            return kind;
        }

        @Override
        public Set<Modifier> getModifiers() {
            return emptySet();
        }

        @Override
        public NameImpl getSimpleName() {
            return name;
        }

        @Override
        public ElementImpl getEnclosingElement() {
            return parent;
        }

        @Override
        public List<ElementImpl> getEnclosedElements() {
            return emptyList();
        }
    }

    public static final class Field extends VariableElementImpl {
        private final FieldNode field;
        private final Set<Modifier> modifiers;
        private final NameImpl name;
        private final TypeElementImpl parent;
        private final MemoizedValue<TypeMirrorImpl> type;

        public Field(Universe universe, TypeElementImpl parent, FieldNode field) {
            super(universe, obtained(AnnotationSource.fromField(field)), parent.lookupModule());
            this.field = field;
            this.modifiers = Modifiers.toFieldModifiers(field.access);
            this.name = NameImpl.of(field.name);
            this.parent = parent;
            this.type = memoize(() -> {
                String sig = field.signature == null ? field.desc : field.signature;
                return TypeMirrorFactory.create(universe, SignatureParser.parseTypeRef(sig), parent, AnnotationTargetPath.ROOT);
            });
        }

        @Override
        public Object getConstantValue() {
            return field.value;
        }

        @Override
        public TypeMirrorImpl asType() {
            return type.get();
        }

        @Override
        public ElementKind getKind() {
            return Modifiers.toFieldElementKind(field.access);
        }

        @Override
        public Set<Modifier> getModifiers() {
            return modifiers;
        }

        @Override
        public NameImpl getSimpleName() {
            return name;
        }

        @Override
        public ElementImpl getEnclosingElement() {
            return parent;
        }

        @Override
        public List<? extends ElementImpl> getEnclosedElements() {
            return emptyList();
        }
    }

    public static final class Parameter extends VariableElementImpl {
        private final ExecutableElementImpl method;
        private final NameImpl name;
        private final Set<Modifier> modifiers;
        private final MemoizedValue<TypeMirrorImpl> type;

        public Parameter(Universe universe, ExecutableElementImpl method, int index) {
            super(universe, obtained(AnnotationSource.fromMethodParameter(method.getNode(), index)),
                    new AnnotationTargetPath(newFormalParameterReference(index)), method.getType().lookupModule());
            this.method = method;
            List<ParameterNode> paramsInfo = method.getNode().parameters;
            ParameterNode node = paramsInfo == null ? null : paramsInfo.get(index);
            this.name = NameImpl.of(node == null ? null : node.name);
            this.modifiers = node == null ? emptySet() : Modifiers.toParameterModifiers(node.access);

            this.type = method.getSignature().map(ms -> {
                TypeSignature paramType = ms.parameterTypes.get(index);
                return TypeMirrorFactory.create(universe, paramType, method,
                        obtained(AnnotationSource.fromMethodParameter(method.getNode(), index)),
                        new AnnotationTargetPath(newFormalParameterReference(index)), method.getType().lookupModule());
            });
        }

        @Override
        public @Nullable Object getConstantValue() {
            return null;
        }

        @Override
        public TypeMirrorImpl asType() {
            return type.get();
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.PARAMETER;
        }

        @Override
        public Set<Modifier> getModifiers() {
            return modifiers;
        }

        @Override
        public NameImpl getSimpleName() {
            return name;
        }

        @Override
        public ElementImpl getEnclosingElement() {
            return method;
        }

        @Override
        public List<? extends ElementImpl> getEnclosedElements() {
            return emptyList();
        }
    }
}
