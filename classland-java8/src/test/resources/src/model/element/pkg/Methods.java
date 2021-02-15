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
package pkg;

import java.lang.annotation.Native;

public class Methods {

    interface DefaultMethods {

        void interfaceMethod();

        default void defaultInterfaceMethod() {}
    }

    static class ElementKinds {
        static {
            int i = 0;
        }

        static {
            int j = 0;
        }

        {
            int i = 1;
        }

        {
            int j = 1;
        }

        ElementKinds(int a) {

        }

        void method() {}
    }

    static class Generics<T> {
        void nonGeneric() {}
        void genericByTypeTypeParam(T a) {}
        <U extends String> void genericByMethodTypeParam(U a) {}
        <U extends T> void methodTypeParamUsesTypeTypeParam(T a, U b) {}

        class Inner<U extends String & Cloneable> {
            void methodGenericFromEnclosingType(T a, U b) {}
        }
    }

    static interface Exceptions {
        void noThrows();
        void throwsChecked() throws Exception;
        void throwsUnchecked() throws RuntimeException;
        <T extends Throwable> void throwsTypeParam() throws T;
        <T extends RuntimeException> void throwsMany() throws Exception, T, Throwable;
    }

    static @interface DefaultValues {
        int defaultPrimitive() default 42;
        String defaultString() default "forty-two";
        Class<?> defaultClass() default Void.class;
        EnumDefaults defaultEnum() default EnumDefaults.DEFAULT;
        Native[] defaultArray() default { @Native, @Native };
        Native defaultAnno() default @Native;
        enum EnumDefaults {
            DEFAULT
        }
    }
}