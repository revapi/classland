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
package org.revapi.classland.impl.model.signature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.revapi.classland.impl.model.element.TypeElementBase;
import org.revapi.classland.impl.util.Nullable;

public final class GenericTypeParameters {
    public final LinkedHashMap<String, TypeParameterBound> typeParameters;
    public final @Nullable TypeSignature superClass;
    public final List<TypeSignature> interfaces;
    public final @Nullable TypeElementBase outerClass;

    public GenericTypeParameters(LinkedHashMap<String, TypeParameterBound> typeParameters,
            @Nullable TypeSignature superClass, List<TypeSignature> interfaces, @Nullable TypeElementBase outerClass) {
        this.typeParameters = typeParameters;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.outerClass = outerClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GenericTypeParameters that = (GenericTypeParameters) o;
        return typeParameters.equals(that.typeParameters) && Objects.equals(superClass, that.superClass)
                && interfaces.equals(that.interfaces) && Objects.equals(outerClass, that.outerClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeParameters, superClass, interfaces, outerClass);
    }

    @Override
    public String toString() {
        return "GenericTypeParameters{" + "typeParameters=" + typeParameters + ", superClass=" + superClass
                + ", interfaces=" + interfaces + ", outerClass=" + outerClass + '}';
    }
}
