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
package org.revapi.classland.impl.model.anno;

import static java.util.Collections.emptyList;

import java.util.List;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.revapi.classland.impl.util.Memoized;

public abstract class AnnotationSource {
    public static final AnnotationSource EMPTY = new AnnotationSource() {
        @Override
        public List<AnnotationNode> getVisibleAnnotations() {
            return emptyList();
        }

        @Override
        public List<AnnotationNode> getInvisibleAnnotations() {
            return emptyList();
        }

        @Override
        public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
            return emptyList();
        }

        @Override
        public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
            return emptyList();
        }
    };

    public static final Memoized<AnnotationSource> MEMOIZED_EMPTY = Memoized.obtained(EMPTY);

    private AnnotationSource() {

    }

    public static AnnotationSource fromType(ClassNode node) {
        return new AnnotationSource() {
            @Override
            public List<AnnotationNode> getVisibleAnnotations() {
                return node.visibleAnnotations == null ? emptyList() : node.visibleAnnotations;
            }

            @Override
            public List<AnnotationNode> getInvisibleAnnotations() {
                return node.invisibleAnnotations == null ? emptyList() : node.invisibleAnnotations;
            }

            @Override
            public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
                return node.visibleTypeAnnotations == null ? emptyList() : node.visibleTypeAnnotations;
            }

            @Override
            public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
                return node.invisibleTypeAnnotations == null ? emptyList() : node.invisibleTypeAnnotations;
            }
        };
    }

    public static AnnotationSource fromMethod(MethodNode node) {
        return new AnnotationSource() {
            @Override
            public List<AnnotationNode> getVisibleAnnotations() {
                return node.visibleAnnotations == null ? emptyList() : node.visibleAnnotations;
            }

            @Override
            public List<AnnotationNode> getInvisibleAnnotations() {
                return node.invisibleAnnotations == null ? emptyList() : node.invisibleAnnotations;
            }

            @Override
            public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
                return node.visibleTypeAnnotations == null ? emptyList() : node.visibleTypeAnnotations;
            }

            @Override
            public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
                return node.invisibleTypeAnnotations == null ? emptyList() : node.invisibleTypeAnnotations;
            }
        };
    }

    public static AnnotationSource fromField(FieldNode node) {
        return new AnnotationSource() {
            @Override
            public List<AnnotationNode> getVisibleAnnotations() {
                return node.visibleAnnotations == null ? emptyList() : node.visibleAnnotations;
            }

            @Override
            public List<AnnotationNode> getInvisibleAnnotations() {
                return node.invisibleAnnotations == null ? emptyList() : node.invisibleAnnotations;
            }

            @Override
            public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
                return node.visibleTypeAnnotations == null ? emptyList() : node.visibleTypeAnnotations;
            }

            @Override
            public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
                return node.invisibleTypeAnnotations == null ? emptyList() : node.invisibleTypeAnnotations;
            }
        };
    }

    // TODO this is not right - the method parameter type annotations are declared differently than
    // the normal annotations..
    // this is the old code I had in VariableElementImpl.Parameter
    // private static List<AnnotationNode> annos(ExecutableElementImpl method, int index) {
    // MethodNode n = method.getNode();
    //
    // int paramCount = n.parameters.size();
    //
    // ArrayList<AnnotationNode> ret = new ArrayList<>();
    // ret.addAll(annos(n.visibleAnnotableParameterCount, paramCount, index, n.visibleParameterAnnotations));
    // ret.addAll(annos(n.invisibleAnnotableParameterCount, paramCount, index, n.invisibleParameterAnnotations));
    // return ret;
    // }
    //
    // private static List<AnnotationNode> annos(int shiftCount, int paramCount, int index,
    // List<AnnotationNode>[] allAnnos) {
    // if (allAnnos == null) {
    // return emptyList();
    // }
    //
    // int indexShift = shiftCount == 0 ? 0 : paramCount - shiftCount;
    //
    // List<AnnotationNode> ret = allAnnos[index - indexShift];
    // return ret == null ? emptyList() : ret;
    // }

    public static AnnotationSource fromMethodParameter(MethodNode node, int paramIndex) {
        return new AnnotationSource() {
            @Override
            public List<AnnotationNode> getVisibleAnnotations() {
                return node.visibleAnnotations == null ? emptyList() : node.visibleAnnotations;
            }

            @Override
            public List<AnnotationNode> getInvisibleAnnotations() {
                return node.invisibleAnnotations == null ? emptyList() : node.invisibleAnnotations;
            }

            @Override
            public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
                return node.visibleTypeAnnotations == null ? emptyList() : node.visibleTypeAnnotations;
            }

            @Override
            public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
                return node.invisibleTypeAnnotations == null ? emptyList() : node.invisibleTypeAnnotations;
            }
        };
    }

    public abstract List<AnnotationNode> getVisibleAnnotations();

    public abstract List<AnnotationNode> getInvisibleAnnotations();

    public abstract List<TypeAnnotationNode> getVisibleTypeAnnotations();

    public abstract List<TypeAnnotationNode> getInvisibleTypeAnnotations();
}
