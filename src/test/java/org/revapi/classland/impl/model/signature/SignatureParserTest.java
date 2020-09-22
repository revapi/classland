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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.revapi.classland.impl.model.signature.Bound.Type.EXACT;
import static org.revapi.classland.impl.model.signature.Bound.Type.EXTENDS;
import static org.revapi.classland.impl.model.signature.Bound.Type.SUPER;
import static org.revapi.classland.impl.model.signature.Bound.Type.UNBOUNDED;

import javax.lang.model.type.TypeKind;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SignatureParserTest {

    @ParameterizedTest
    @MethodSource("fields")
    void fields(String signature, TypeSignature expected) {
        TypeSignature sig = SignatureParser.parseField(signature);
        assertEquals(expected, sig);
    }

    static Object[][] fields() {
        return new Object[][] { { "[Z", new TypeSignature.PrimitiveType(1, TypeKind.BOOLEAN) },
                { "Ljava/lang/Object;", new TypeSignature.Reference(0, "java/lang/Object", emptyList(), null) },
                { "TA;", new TypeSignature.Variable(0, "A") },
                { "[Ljava/util/List<[[TC;>;",
                        new TypeSignature.Reference(1, "java/util/List",
                                singletonList(new Bound(EXACT, new TypeSignature.Variable(2, "C"))), null) },
                { "Ljava/util/List<-[[TD;>;",
                        new TypeSignature.Reference(0, "java/util/List",
                                singletonList(new Bound(SUPER, new TypeSignature.Variable(2, "D"))), null) },
                { "Ljava/util/List<+TE;>;",
                        new TypeSignature.Reference(0, "java/util/List",
                                singletonList(new Bound(EXTENDS, new TypeSignature.Variable(0, "E"))), null) },
                { "Ljava/util/List<*>;",
                        new TypeSignature.Reference(0, "java/util/List", singletonList(new Bound(UNBOUNDED, null)),
                                null) },
                { "Ljava/util/List<Ljava/lang/String;>;", new TypeSignature.Reference(0, "java/util/List",
                        singletonList(new Bound(EXACT,
                                new TypeSignature.Reference(0, "java/lang/String", emptyList(), null))),
                        null) },
                { "Lpkg/Top.Outer<TB;>.Inner<[Ljava/lang/String;TC;>;", new TypeSignature.Reference(0,
                        "pkg/Top$Outer$Inner",
                        asList(new Bound(EXACT, new TypeSignature.Reference(1, "java/lang/String", emptyList(), null)),
                                new Bound(EXACT, new TypeSignature.Variable(0, "C"))),
                        new TypeSignature.Reference(0, "pkg/Top$Outer",
                                singletonList(new Bound(EXACT, new TypeSignature.Variable(0, "B"))),
                                new TypeSignature.Reference(0, "pkg/Top", emptyList(), null))) },

        };
    }
}
