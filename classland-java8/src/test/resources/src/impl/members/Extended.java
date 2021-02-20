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