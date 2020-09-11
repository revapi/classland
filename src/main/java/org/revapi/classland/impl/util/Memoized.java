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

import java.util.function.Function;
import java.util.function.Supplier;

public final class Memoized<T> implements Supplier<T> {
    private final Supplier<T> action;
    private volatile boolean obtained;
    private T value;

    public Memoized(Supplier<T> action) {
        this.action = action;
    }

    public static <T> Memoized<T> memoize(Supplier<T> action) {
        if (action instanceof Memoized) {
            return (Memoized<T>) action;
        } else {
            return new Memoized<>(action);
        }
    }

    public boolean isObtained() {
        return obtained;
    }

    @Override
    public T get() {
        if (obtained) {
            return value;
        }

        synchronized (this) {
            if (obtained) {
                return value;
            }

            value = action.get();

            obtained = true;

            return value;
        }
    }

    public <U> Memoized<U> map(Function<T, U> action) {
        return new Memoized<>(() -> {
            T val = get();
            return action.apply(val);
        });
    }

    @Override
    public String toString() {
        return "Memoized{" + (obtained ? ("value=" + value) : "<pending>") + "}";
    }
}
