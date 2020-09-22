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
}