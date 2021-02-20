package annoattrdefaults;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface Anno {
    int value();

    int intDefault() default 42;

    Class<?> classDefault() default Void.class;

    int[] arrayDefault() default {42, 43};
}