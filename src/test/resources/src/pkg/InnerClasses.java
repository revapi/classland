package pkg;

public class InnerClasses {

    public static class StaticMember {
        public void method() {
            class Local {}
        }

        Object anonymous = new Object() {};
    }

    public class InstanceMember {
        public void method() { class Local {} }

        Object anonymous = new Object() {};
    }
}