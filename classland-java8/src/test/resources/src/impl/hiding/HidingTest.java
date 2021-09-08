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
package hiding;

public class HidingTest {

    public static class StaticBase {
        public static int field;
        public static int method() {return 0;}
        public static Object covariantMethod() { return null; }
        public static class Class {}
    }

    public static class StaticHider extends StaticBase {
        public static float field;
        public static int method() { return 0; }
        public static String covariantMethod() { return null; }
        public static enum Class {}
    }

    public static class InstanceBase {
        public int field;
        public void method() {}
        public Object covariantMethod() { return null; }
        public class Class {}
    }

    public static class InstanceHider extends InstanceBase {
        public float field;
        public int method(int i) { return 0; }
        public String covariantMethod() { return null; }
        public class Class {}
    }

    public static class StaticInstanceBase {
        public static int field;
        public static int method() {return 0;}
        // instance method cannot override a static method
        //public static Object covariantMethod() { return null; }
        public static class Class {}
    }

    public static class StaticInstanceHider extends StaticInstanceBase {
        public float field;
        public int method(int i) { return 0; }
        // instance method cannot override a static method
        //public String covariantMethod() { return null; }
        public class Class {}
    }

    public static class InstanceStaticBase {
        public int field;
        // a static method cannot override an instance method - this would not compile
        //public void method() {}
        //public Object covariantMethod() { return null; }
        public class Class {}
    }

    public static class InstanceStaticHider extends InstanceStaticBase {
        public static float field;
        // a static method cannot override an instance method - this would not compile
        //public static int method() { return 0; }
        //public static String covariantMethod() { return null; }
        public static enum Class {}
    }
}