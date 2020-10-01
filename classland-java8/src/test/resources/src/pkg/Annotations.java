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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

public class Annotations {

    @Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, MODULE, PACKAGE, PARAMETER, TYPE,
            TYPE_PARAMETER, TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VisibleTypeAnno {
    }

    @Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, MODULE, PACKAGE, PARAMETER, TYPE,
            TYPE_PARAMETER, TYPE_USE})
    @Retention(RetentionPolicy.CLASS)
    public @interface InvisibleTypeAnno {
    }

    @Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, MODULE, PACKAGE, PARAMETER, TYPE,
            TYPE_PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VisibleAnno {
    }

    @Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, MODULE, PACKAGE, PARAMETER, TYPE,
            TYPE_PARAMETER})
    @Retention(RetentionPolicy.CLASS)
    public @interface InvisibleAnno {
    }

    @VisibleAnno @InvisibleTypeAnno
    public class AnnotatedClass {}

    public class AnnotatedTypeParameter<@VisibleTypeAnno @InvisibleTypeAnno T, @InvisibleTypeAnno U> {}

    public class AnnotatedMethodParameter {
        public AnnotatedMethodParameter(@VisibleAnno @VisibleTypeAnno Object explicitParam) {}
        void method(@VisibleAnno @VisibleTypeAnno int p, @InvisibleTypeAnno double q, String r, @VisibleTypeAnno Object s) {}
    }

    public class AnnotatedMethodParameterTypeVariable {
        void method(java.util.Set<@InvisibleTypeAnno String> p) {}
    }

    public class AnnotatedMethod {
        @VisibleAnno @VisibleTypeAnno
        void method() {}
    }

    public class AnnotatedReceiverType {
        AnnotatedReceiverType() {}
        AnnotatedReceiverType(@InvisibleTypeAnno Annotations Annotations.this, @InvisibleTypeAnno double param) {}
        void method(@VisibleTypeAnno AnnotatedReceiverType this, @VisibleTypeAnno int param) {}
        void method(double param) {}
    }
}