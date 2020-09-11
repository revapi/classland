package pkg;

public class Fields {
    static int staticWithoutValue = 1;
    static final int staticWithValue = 2;

    enum Enum {
        VARIANT1;
        static final Enum normalField = VARIANT1;
    }
}