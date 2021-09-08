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
package members;

public class Base {
    public Base() {}
    public Base(int param) {}
    protected Base(float param) {}
    private Base(String param) {}

    public static int publicStaticFieldBase;
    protected static int protectedStaticFieldBase;
    private static int privateStaticFieldBase;

    public int publicFieldBase;
    protected int protectedFieldBase;
    private int privateFieldBase;

    public void publicMethodBase() {}
    protected void protectedMethodBase() {}
    private void privateMethodBase() {}

    public static void publicStaticMethodBase() {}
    protected static void protectedStaticMethodBase() {}
    private static void privateStaticMethodBase() {}

    public class PublicInnerClassBase {}
    protected class ProtectedInnerClassBase {}
    private class PrivateInnerClassBase {}

    public static class PublicInnerStaticClassBase {}
    protected static class ProtectedInnerStaticClassBase {}
    private static class PrivateInnerStaticClassBase {}
}
