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

import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.revapi.classland.impl.util.Nullable;

public class AnnotationTargetPath implements Cloneable {
    public static final AnnotationTargetPath ROOT = new AnnotationTargetPath(null);

    public final @Nullable TypeReference ref;
    private final StringBuilder typePathSteps = new StringBuilder();

    public AnnotationTargetPath(@Nullable TypeReference ref) {
        this.ref = ref;
    }

    public AnnotationTargetPath(int typeReferenceSort) {
        this(TypeReference.newTypeReference(typeReferenceSort));
    }

    public AnnotationTargetPath array() {
        typePathSteps.append('[');
        return this;
    }

    public AnnotationTargetPath innerType() {
        typePathSteps.append('.');
        return this;
    }

    public AnnotationTargetPath wildcardBound() {
        typePathSteps.append('*');
        return this;
    }

    public AnnotationTargetPath typeArgument(int index) {
        typePathSteps.append(index).append(';');
        return this;
    }

    public TypePath stepsAsTypePath() {
        return TypePath.fromString(typePathSteps.toString());
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public AnnotationTargetPath clone() {
        AnnotationTargetPath ret = new AnnotationTargetPath(ref);
        ret.typePathSteps.append(this.typePathSteps);
        return ret;
    }
}
