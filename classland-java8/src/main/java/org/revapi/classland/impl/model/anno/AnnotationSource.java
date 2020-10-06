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
import java.util.stream.Collectors;

import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

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

    public static final MemoizedValue<AnnotationSource> MEMOIZED_EMPTY = MemoizedValue.obtained(EMPTY);

    private AnnotationSource() {

    }

    private static <T> List<T> nonNullOrEmpty(@Nullable List<T> list) {
        return list == null ? emptyList() : list;
    }

    public static AnnotationSource fromType(ClassNode node) {
        return new AnnotationSource() {
            @Override
            public List<AnnotationNode> getVisibleAnnotations() {
                return nonNullOrEmpty(node.visibleAnnotations);
            }

            @Override
            public List<AnnotationNode> getInvisibleAnnotations() {
                return nonNullOrEmpty(node.invisibleAnnotations);
            }

            @Override
            public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
                return nonNullOrEmpty(node.visibleTypeAnnotations);
            }

            @Override
            public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
                return nonNullOrEmpty(node.invisibleTypeAnnotations);
            }
        };
    }

    public static AnnotationSource fromMethod(MethodNode node) {
        return new AnnotationSource() {
            @Override
            public List<AnnotationNode> getVisibleAnnotations() {
                return nonNullOrEmpty(node.visibleAnnotations);
            }

            @Override
            public List<AnnotationNode> getInvisibleAnnotations() {
                return nonNullOrEmpty(node.invisibleAnnotations);
            }

            @Override
            public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
                return nonNullOrEmpty(node.visibleTypeAnnotations);
            }

            @Override
            public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
                return nonNullOrEmpty(node.invisibleTypeAnnotations);
            }
        };
    }

    public static AnnotationSource fromField(FieldNode node) {
        return new AnnotationSource() {
            @Override
            public List<AnnotationNode> getVisibleAnnotations() {
                return nonNullOrEmpty(node.visibleAnnotations);
            }

            @Override
            public List<AnnotationNode> getInvisibleAnnotations() {
                return nonNullOrEmpty(node.invisibleAnnotations);
            }

            @Override
            public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
                return nonNullOrEmpty(node.visibleTypeAnnotations);
            }

            @Override
            public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
                return nonNullOrEmpty(node.invisibleTypeAnnotations);
            }
        };
    }

    public static AnnotationSource fromMethodParameter(MethodNode node, int paramIndex) {
        int paramCount = Type.getMethodType(node.desc).getArgumentTypes().length;
        return new AnnotationSource() {
            @Override
            public List<AnnotationNode> getVisibleAnnotations() {
                if (node.visibleParameterAnnotations == null) {
                    return emptyList();
                } else {
                    return nonNullOrEmpty(
                            node.visibleParameterAnnotations[shiftedIndex(node.visibleAnnotableParameterCount)]);
                }
            }

            @Override
            public List<AnnotationNode> getInvisibleAnnotations() {
                if (node.invisibleParameterAnnotations == null) {
                    return emptyList();
                } else {
                    return nonNullOrEmpty(
                            node.invisibleParameterAnnotations[shiftedIndex(node.invisibleAnnotableParameterCount)]);
                }
            }

            @Override
            public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
                return filterOutFormalParameterAnnotations(nonNullOrEmpty(node.visibleTypeAnnotations));
            }

            @Override
            public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
                return filterOutFormalParameterAnnotations(nonNullOrEmpty(node.invisibleTypeAnnotations));
            }

            private int shiftedIndex(int shift) {
                int indexShift = shift == 0 ? 0 : (paramCount - shift);
                return paramIndex - indexShift;
            }

            /**
             * We need to filter out the type annotations of the "method formal parameter" sort with no typepath because
             * those also appear in the list of "normal" annotations.
             */
            private List<TypeAnnotationNode> filterOutFormalParameterAnnotations(List<TypeAnnotationNode> list) {
                return list.stream()
                        // the sort of the type reference is computed by >>> 24 bitshift. There is no static method
                        // for that in ASM that I know of, so we need to play magic here...
                        .filter(n -> (TypeReference.METHOD_FORMAL_PARAMETER & (n.typeRef >>> 24)) == 0
                                || n.typePath != null)
                        .collect(Collectors.toList());
            }
        };
    }

    public abstract List<AnnotationNode> getVisibleAnnotations();

    public abstract List<AnnotationNode> getInvisibleAnnotations();

    public abstract List<TypeAnnotationNode> getVisibleTypeAnnotations();

    public abstract List<TypeAnnotationNode> getInvisibleTypeAnnotations();
}
