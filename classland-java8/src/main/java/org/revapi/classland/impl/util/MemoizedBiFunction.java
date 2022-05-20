/*
 * Copyright 2020-2022 Lukas Krejci
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

import static org.revapi.classland.impl.util.MemoizedValue.DEBUG;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class MemoizedBiFunction<T, U, R> implements BiFunction<T, U, R> {
    private final @Nullable BiFunction<T, U, R> action;
    protected Map<Pair<T, U>, R> values;

    private MemoizedBiFunction(@Nullable BiFunction<T, U, R> action) {
        this.action = action;
        values = new ConcurrentHashMap<>();
    }

    private static <T, U, R> MemoizedBiFunction<T, U, R> instantiate(@Nullable BiFunction<T, U, R> action) {
        if (DEBUG) {
            return new MemoizedBiFunction.Debug<>(action);
        } else {
            return new MemoizedBiFunction<>(action);
        }
    }

    public static <T, U, R> MemoizedBiFunction<T, U, R> memoize(BiFunction<T, U, R> action) {
        if (action instanceof MemoizedBiFunction) {
            return (MemoizedBiFunction<T, U, R>) action;
        } else {
            return instantiate(action);
        }
    }

    @Override
    public R apply(T a, U b) {
        return values.computeIfAbsent(new Pair<>(a, b), __ -> action.apply(a, b));
    }

    private static final class Pair<T, U> {
        final T first;
        final U second;

        Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pair)) {
                return false;
            }

            Pair<?, ?> pair = (Pair<?, ?>) o;

            if (!Objects.equals(first, pair.first)) {
                return false;
            }

            return Objects.equals(second, pair.second);
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            return result;
        }
    }

    private static final class Debug<T, U, R> extends MemoizedBiFunction<T, U, R> {
        private final Throwable instantiationLocation;

        private Debug(@Nullable BiFunction<T, U, R> action) {
            super(action);
            instantiationLocation = new Throwable();
        }

        @Override
        public R apply(T a, U b) {
            try {
                return super.apply(a, b);
            } catch (Exception e) {
                throw new RuntimeException("Memoized call allocated at " + location() + " failed.", e);
            }
        }

        private String location() {
            StackTraceElement el = instantiationLocation.getStackTrace()[4];
            return el.getClassName() + "." + el.getMethodName() + "(" + el.getFileName() + ":" + el.getLineNumber()
                    + ")";
        }

        @Override
        public String toString() {
            return "MemoizedBiFunction{size=" + values.size() + " @ " + location() + "}";
        }
    }
}
