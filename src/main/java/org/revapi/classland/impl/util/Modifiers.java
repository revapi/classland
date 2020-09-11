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
package org.revapi.classland.impl.util;

import java.util.EnumSet;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

import org.objectweb.asm.Opcodes;

public final class Modifiers {
    private Modifiers() {

    }

    public static Set<Modifier> toTypeModifiers(int flags) {
        Set<Modifier> ret = EnumSet.noneOf(Modifier.class);

        if (hasFlag(flags, Opcodes.ACC_PUBLIC))
            ret.add(Modifier.PUBLIC);

        if (hasFlag(flags, Opcodes.ACC_PROTECTED))
            ret.add(Modifier.PROTECTED);

        if (hasFlag(flags, Opcodes.ACC_PRIVATE))
            ret.add(Modifier.PRIVATE);

        if (hasFlag(flags, Opcodes.ACC_ABSTRACT))
            ret.add(Modifier.ABSTRACT);

        if (hasFlag(flags, Opcodes.ACC_STATIC))
            ret.add(Modifier.STATIC);

        if (hasFlag(flags, Opcodes.ACC_FINAL))
            ret.add(Modifier.FINAL);

        if (hasFlag(flags, Opcodes.ACC_STRICT))
            ret.add(Modifier.STRICTFP);

        return ret;
    }

    public static ElementKind toTypeElementKind(int flags) {
        if (hasFlag(flags, Opcodes.ACC_INTERFACE)) {
            if (hasFlag(flags, Opcodes.ACC_ANNOTATION)) {
                return ElementKind.ANNOTATION_TYPE;
            } else {
                return ElementKind.INTERFACE;
            }
        }

        if (hasFlag(flags, Opcodes.ACC_ENUM))
            return ElementKind.ENUM;

        return ElementKind.CLASS;
    }

    private static boolean hasFlag(int flags, int flag) {
        return (flags & flag) == flag;
    }
}
