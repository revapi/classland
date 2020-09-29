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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.revapi.classland.impl.util.Nullable;

public class AnnotationFinder {

    private AnnotationFinder() {
    }

    public static List<AnnotationNode> find(AnnotationTargetPath path, AnnotationSource annotationSource) {
        if (path.ref == null) {
            ArrayList<AnnotationNode> ret = new ArrayList<>(annotationSource.getVisibleAnnotations());
            ret.addAll(annotationSource.getInvisibleAnnotations());
            return ret;
        } else {
            TypeReference ref = path.ref;
            TypePath p = path.stepsAsTypePath();
            Stream<TypeAnnotationNode> typeAnnos = Stream.concat(annotationSource.getVisibleTypeAnnotations().stream(),
                    annotationSource.getInvisibleTypeAnnotations().stream()).filter(a -> matches(a, ref, p));

            Stream<AnnotationNode> annos = Stream.concat(annotationSource.getVisibleAnnotations().stream(),
                    annotationSource.getInvisibleAnnotations().stream());

            return Stream.concat(annos, typeAnnos).collect(toList());
        }
    }

    private static boolean matches(TypeAnnotationNode anno, TypeReference ref, @Nullable TypePath path) {
        return anno.typeRef == ref.getValue() && ((anno.typePath == null && path == null)
                || (anno.typePath != null && path != null && anno.typePath.toString().equals(path.toString())));
    }
}
