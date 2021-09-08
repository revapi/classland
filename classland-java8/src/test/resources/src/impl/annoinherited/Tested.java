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
package annoinherited;

import java.lang.annotation.Inherited;

public class Tested {

    @Inherited
    public @interface Inheritable {
        int a() default 0;
    }

    public @interface NonInheritable {
        int a() default 0;
    }

    @Inheritable
    @NonInheritable
    public class Base {}

    public class InheritFromBase extends Base {}

    @NonInheritable(a = 1)
    public class InheritFromBaseAndOwnAnno extends Base {}

    @Inheritable(a = 1)
    public class OverrideFromBase extends Base {}

    @Inheritable(a = 1)
    @NonInheritable(a = 1)
    public class OverrideFromBaseAndOwnAnno extends Base {}
}