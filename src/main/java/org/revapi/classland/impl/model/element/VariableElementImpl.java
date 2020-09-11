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

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.revapi.classland.impl.model.NameImpl;
import org.revapi.classland.impl.model.Universe;
import org.revapi.classland.impl.model.mirror.AnnotationMirrorImpl;
import org.revapi.classland.impl.util.Memoized;
import org.revapi.classland.impl.util.Modifiers;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.revapi.classland.impl.util.Memoized.memoize;

public abstract class VariableElementImpl extends ElementImpl implements VariableElement {
    private final Memoized<List<AnnotationMirrorImpl>> annos;

    @SafeVarargs
    private VariableElementImpl(Universe universe, List<? extends AnnotationNode>... annos) {
        super(universe);

        this.annos = memoize(() -> parseMoreAnnotations(annos));
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitVariable(this, p);
    }

    @Override
    public List<AnnotationMirrorImpl> getAnnotationMirrors() {
        return annos.get();
    }

    public static final class Field extends VariableElementImpl {
        private final FieldNode field;
        private final Set<Modifier> modifiers;
        private final NameImpl name;
        private final TypeElementImpl parent;

        public Field(Universe universe, TypeElementImpl parent, FieldNode field) {
            super(universe, field.visibleAnnotations, field.invisibleAnnotations, field.visibleTypeAnnotations,
                    field.invisibleTypeAnnotations);
            this.field = field;
            this.modifiers = Modifiers.toFieldModifiers(field.access);
            this.name = NameImpl.of(field.name);
            this.parent = parent;
        }

        @Override
        public Object getConstantValue() {
            return field.value;
        }

        @Override
        public TypeMirror asType() {
            // TODO implement
            return null;
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
        public Name getSimpleName() {
            return name;
        }

        @Override
        public Element getEnclosingElement() {
            return parent;
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            return emptyList();
        }
    }

    public static final class Parameter extends VariableElementImpl {
        private final ExecutableElementImpl parent;
        private final NameImpl name;
        private final Set<Modifier> modifiers;

        public Parameter(Universe universe, ExecutableElementImpl parent, int index) {
            super(universe, annos(parent, index));
            this.parent = parent;
            ParameterNode node = parent.getNode().parameters.get(index);
            this.name = NameImpl.of(node.name);
            this.modifiers = Modifiers.toParameterModifiers(node.access);
        }

        private static List<AnnotationNode> annos(ExecutableElementImpl method, int index) {
            MethodNode n = method.getNode();

            int paramCount = n.parameters.size();

            ArrayList<AnnotationNode> ret = new ArrayList<>();
            ret.addAll(annos(n.visibleAnnotableParameterCount, paramCount, index, n.visibleParameterAnnotations));
            ret.addAll(annos(n.invisibleAnnotableParameterCount, paramCount, index, n.invisibleParameterAnnotations));
            return ret;
        }

        private static List<AnnotationNode> annos(int shiftCount, int paramCount, int index, List<AnnotationNode>[] allAnnos) {
            if (allAnnos == null) {
                return emptyList();
            }

            int indexShift = shiftCount == 0 ? 0 : paramCount - shiftCount;

            List<AnnotationNode> ret = allAnnos[index - indexShift];
            return ret == null ? emptyList() : ret;
        }
        @Override
        public Object getConstantValue() {
            return null;
        }

        @Override
        public TypeMirror asType() {
            // TODO implement
            return null;
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
        public Name getSimpleName() {
            return name;
        }

        @Override
        public Element getEnclosingElement() {
            return parent;
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            return emptyList();
        }
    }
}
