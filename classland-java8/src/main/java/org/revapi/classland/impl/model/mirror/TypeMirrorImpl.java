/*
 * Copyright 2020-2021 Lukas Krejci
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

import java.util.List;

import javax.lang.model.type.TypeMirror;

import org.revapi.classland.impl.Universe;
import org.revapi.classland.impl.model.AnnotatedConstructImpl;
import org.revapi.classland.impl.model.anno.AnnotationSource;
import org.revapi.classland.impl.model.anno.AnnotationTargetPath;
import org.revapi.classland.impl.model.element.ModuleElementImpl;
import org.revapi.classland.impl.util.MemoizedValue;
import org.revapi.classland.impl.util.Nullable;

public abstract class TypeMirrorImpl extends AnnotatedConstructImpl implements TypeMirror {
    protected TypeMirrorImpl(Universe universe, MemoizedValue<AnnotationSource> annotationSource,
            AnnotationTargetPath path, MemoizedValue<@Nullable ModuleElementImpl> typeLookupSeed) {
        super(universe, annotationSource, path, typeLookupSeed, true);
    }

    protected TypeMirrorImpl(Universe universe, MemoizedValue<List<AnnotationMirrorImpl>> annos) {
        super(universe, annos);
    }

    // type mirrors are generated on demand and can exist in multiple copies
    // we therefore need to have a semantic equals&hashcode as opposed
    // to the quick identity checks in elements

    @Override
    public int hashCode() {
        return 31 * universe.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!getClass().equals(obj.getClass())) {
            return false;
        }

        TypeMirrorImpl that = (TypeMirrorImpl) obj;
        return universe.equals(that.universe);
    }
}
