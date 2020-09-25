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

import java.util.Objects;

public class Bound {
    public final Type boundType;
    public final TypeSignature type;

    public Bound(Type boundType, TypeSignature type) {
        this.boundType = boundType;
        this.type = type;
    }

    public enum Type {
        EXACT, EXTENDS, SUPER, UNBOUNDED;

        public static Type fromWildcardDescriptor(char wildcard) {
            switch (wildcard) {
            case '=':
                return EXACT;
            case '-':
                return SUPER;
            case '+':
                return EXTENDS;
            default:
                return null;
            }
        }

        @Override
        public String toString() {
            switch (this) {
            case EXACT:
                return "=";
            case EXTENDS:
                return "extends";
            case SUPER:
                return "super";
            case UNBOUNDED:
                return "?";
            default:
                throw new IllegalStateException("Unhandled bound type.");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Bound bound = (Bound) o;
        return boundType == bound.boundType && Objects.equals(type, bound.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundType, type);
    }

    @Override
    public String toString() {
        switch (boundType) {
        case UNBOUNDED:
            return "?";
        case EXACT:
            return type.toString();
        default:
            return "? " + boundType + " " + type;
        }
    }
}
