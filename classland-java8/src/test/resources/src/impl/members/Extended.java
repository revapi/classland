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

public class Extended extends Base {
    public Extended() {
        super();
    }
    protected Extended(Void param) {
        super();
    }
    private Extended(Cloneable param) {
        super();
    }
    
    public static int publicStaticFieldExtended;
    protected static int protectedStaticFieldExtended;
    private static int privateStaticFieldExtended;

    public int publicFieldExtended;
    protected int protectedFieldExtended;
    private int privateFieldExtended;

    public void publicMethodExtended() {}
    protected void protectedMethodExtended() {}
    private void privateMethodExtended() {}

    public static void publicStaticMethodExtended() {}
    protected static void protectedStaticMethodExtended() {}
    private static void privateStaticMethodExtended() {}

    public class PublicInnerClassExtended {}
    protected class ProtectedInnerClassExtended {}
    private class PrivateInnerClassExtended {}

    public static class PublicInnerStaticClassExtended {}
    protected static class ProtectedInnerStaticClassExtended {}
    private static class PrivateInnerStaticClassExtended {}
}