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
