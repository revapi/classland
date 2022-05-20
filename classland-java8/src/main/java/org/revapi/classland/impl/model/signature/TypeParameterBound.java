/*
 * Copyright 2020-2022 Lukas Krejci
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
package org.revapi.classland.impl.model.signature;

import java.util.List;
import java.util.Objects;

import org.revapi.classland.impl.util.Nullable;

public class TypeParameterBound {
    public final Bound.Type boundType;
    public final @Nullable TypeSignature classBound;
    public final List<TypeSignature> interfaceBounds;

    public TypeParameterBound(Bound.Type boundType, @Nullable TypeSignature classBound,
            List<TypeSignature> interfaceBounds) {
        this.boundType = boundType;
        this.classBound = classBound;
        this.interfaceBounds = interfaceBounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypeParameterBound that = (TypeParameterBound) o;
        return boundType == that.boundType && Objects.equals(classBound, that.classBound)
                && interfaceBounds.equals(that.interfaceBounds);
    }

    @Override
    public int hashCode() {
        return 31 * (boundType.hashCode() + Objects.hashCode(classBound) + interfaceBounds.hashCode());
    }

    @Override
    public String toString() {
        return "TypeParameterBound{" + "boundType=" + boundType + ", classBound=" + classBound + ", interfaceBounds="
                + interfaceBounds + '}';
    }
}
