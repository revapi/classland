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

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;

import javax.lang.model.type.TypeKind;

import org.revapi.classland.impl.util.Nullable;

public abstract class TypeSignature {
    private TypeSignature() {

    }

    public abstract <R, P> R accept(Visitor<R, P> visitor, P param);

    public static abstract class Arrayable extends TypeSignature {
        public final int arrayDimension;

        private Arrayable(int arrayDimension) {
            this.arrayDimension = arrayDimension;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Arrayable arrayable = (Arrayable) o;
            return arrayDimension == arrayable.arrayDimension;
        }

        @Override
        public int hashCode() {
            return Objects.hash(arrayDimension);
        }

        @Override
        public String toString() {
            if (arrayDimension == 0) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arrayDimension; ++i) {
                sb.append("[]");
            }

            return sb.toString();
        }
    }

    public static final class PrimitiveType extends Arrayable {
        public final TypeKind type;

        public PrimitiveType(int arrayDimension, TypeKind type) {
            super(arrayDimension);
            this.type = type;
            if (!type.isPrimitive()) {
                throw new IllegalArgumentException("A primitive type constructed with a non-primitive class.");
            }
        }

        @Override
        public <R, P> R accept(Visitor<R, P> visitor, P param) {
            return visitor.visitPrimitiveType(this, param);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            if (!super.equals(o))
                return false;
            PrimitiveType that = (PrimitiveType) o;
            return type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), type);
        }

        @Override
        public String toString() {
            return type + super.toString();
        }
    }

    public static final class Variable extends Arrayable {
        public final String name;

        public Variable(int arrayDimension, String name) {
            super(arrayDimension);
            this.name = name;
        }

        @Override
        public <R, P> R accept(Visitor<R, P> visitor, P param) {
            return visitor.visitTypeVariable(this, param);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            if (!super.equals(o))
                return false;
            Variable variable = (Variable) o;
            return name.equals(variable.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), name);
        }

        @Override
        public String toString() {
            return name + super.toString();
        }
    }

    public static final class Reference extends Arrayable {
        public final String internalTypeName;
        public final List<Bound> typeArguments;
        public final @Nullable TypeSignature outerClass;

        public Reference(int arrayDimension, String internalTypeName, List<Bound> typeArguments,
                @Nullable TypeSignature outerClass) {
            super(arrayDimension);
            this.internalTypeName = internalTypeName;
            this.typeArguments = typeArguments;
            this.outerClass = outerClass;
        }

        @Override
        public <R, P> R accept(Visitor<R, P> visitor, P param) {
            return visitor.visitType(this, param);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            if (!super.equals(o))
                return false;
            Reference reference = (Reference) o;
            return internalTypeName.equals(reference.internalTypeName) && typeArguments.equals(reference.typeArguments)
                    && Objects.equals(outerClass, reference.outerClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), internalTypeName, typeArguments, outerClass);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (outerClass != null) {
                sb.append(outerClass.toString());
                sb.append(".");
            }
            sb.append(internalTypeName);
            if (typeArguments != null && !typeArguments.isEmpty()) {
                sb.append("<");
                for (Bound b : typeArguments) {
                    sb.append(b);
                    sb.append(",");
                }
                sb.replace(sb.length() - 1, sb.length(), ">");
            }

            sb.append(super.toString());

            return sb.toString();
        }
    }

    public interface Visitor<R, P> {
        R visitPrimitiveType(PrimitiveType type, P param);

        R visitTypeVariable(Variable typeVariable, P param);

        R visitType(Reference typeReference, P param);
    }
}
