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
import java.util.Optional;

import org.revapi.classland.impl.model.element.TypeElementImpl;

public class GenericMethodParameters {
    public final LinkedHashMap<String, TypeParameterBound> typeParameters;
    public final TypeSignature returnType;
    public final List<TypeSignature> parameterTypes;
    public final List<TypeSignature> exceptionTypes;

    public GenericMethodParameters(LinkedHashMap<String, TypeParameterBound> typeParameters, TypeSignature returnType,
            List<TypeSignature> parameterTypes, List<TypeSignature> exceptionTypes) {
        this.typeParameters = typeParameters;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.exceptionTypes = exceptionTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GenericMethodParameters that = (GenericMethodParameters) o;
        return typeParameters.equals(that.typeParameters) && returnType.equals(that.returnType)
                && parameterTypes.equals(that.parameterTypes) && exceptionTypes.equals(that.exceptionTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeParameters, returnType, parameterTypes, exceptionTypes);
    }

    @Override
    public String toString() {
        return "GenericMethodParameters{" + "typeParameters=" + typeParameters + ", returnType=" + returnType
                + ", parameterTypes=" + parameterTypes + ", exceptionTypes=" + exceptionTypes + '}';
    }
}
