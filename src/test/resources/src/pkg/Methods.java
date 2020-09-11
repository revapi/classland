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