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
package org.revapi.classland.impl.util;

import java.util.function.Supplier;

public class Exceptions {
    private Exceptions() {

    }

    public static <T, E extends Throwable> T failWithRuntimeException(Throwing<T, E> action) {
        try {
            return action.call();
        } catch (Throwable e) {
            return sneakyThrow(e);
        }
    }

    public static <T, E extends Throwable> Supplier<T> callWithRuntimeException(Throwing<T, E> action) {
        return () -> failWithRuntimeException(action);
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    public interface Throwing<T, E extends Throwable> {
        T call() throws E;
    }
}
