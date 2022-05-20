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

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class MemoizedValue<T> implements Supplier<T> {
    static final boolean DEBUG = "true".equals(System.getenv("CLASSLAND_MEMOIZATION_DEBUG"));
    private static final MemoizedValue<?> NULL = obtained(null);
    private static final MemoizedValue<?> EMPTY_LIST = obtained(Collections.emptyList());

    private @Nullable Supplier<T> action;
    protected volatile boolean obtained;
    protected T value;

    private MemoizedValue(@Nullable Supplier<T> action) {
        this.action = action;
    }

    private static <T> MemoizedValue<T> instantiate(@Nullable Supplier<T> action) {
        if (DEBUG) {
            return new Debug<>(action);
        } else {
            return new MemoizedValue<>(action);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> MemoizedValue<T> obtainedNull() {
        return (MemoizedValue<T>) NULL;
    }

    @SuppressWarnings("unchecked")
    public static <T> MemoizedValue<List<T>> obtainedEmptyList() {
        return (MemoizedValue<List<T>>) EMPTY_LIST;
    }

    public static <T> MemoizedValue<T> memoize(Supplier<T> action) {
        if (action instanceof MemoizedValue) {
            return (MemoizedValue<T>) action;
        } else {
            return instantiate(action);
        }
    }

    public static <T> MemoizedValue<T> obtained(T value) {
        MemoizedValue<T> ret = instantiate(null);
        ret.obtained = true;
        ret.value = value;
        return ret;
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

            assert action != null;
            value = action.get();

            obtained = true;

            action = null;

            return value;
        }
    }

    public T swap(T newValue) {
        synchronized (this) {
            T val = get();
            this.value = newValue;
            return val;
        }
    }

    public <U> MemoizedValue<U> map(Function<T, U> action) {
        return instantiate(() -> {
            T val = get();
            return action.apply(val);
        });
    }

    @Override
    public String toString() {
        return "Memoized{" + (obtained ? ("value=" + value) : "<pending>") + "}";
    }

    private static final class Debug<T> extends MemoizedValue<T> {
        private final Throwable instantiationLocation;

        private Debug(@Nullable Supplier<T> action) {
            super(action);
            instantiationLocation = new Throwable();
        }

        @Override
        public T get() {
            try {
                return super.get();
            } catch (Exception e) {
                throw new RuntimeException("Memoized call allocated at " + location() + " failed.", e);
            }
        }

        @Override
        public T swap(T newValue) {
            synchronized (this) {
                instantiationLocation.addSuppressed(new Throwable(obtained ? "o" : ""));
                return super.swap(newValue);
            }
        }

        private static String location(Throwable t, int stackDepth) {
            StackTraceElement el = t.getStackTrace()[stackDepth];
            return el.getClassName() + "." + el.getMethodName() + "(" + el.getFileName() + ":" + el.getLineNumber()
                    + ")";
        }

        private String location() {
            return location(instantiationLocation, 4);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Memoized{");
            if (obtained) {
                sb.append("value=").append(value);
            } else {
                sb.append("<pending>");
            }

            sb.append(" @ ").append(location());

            Throwable[] swaps = instantiationLocation.getSuppressed();
            if (swaps.length > 0) {
                if (!"o".equals(swaps[0].getMessage())) {
                    sb.append(" <swapped-unevaluated>");
                }
                sb.append(", <swap> ").append(location(swaps[swaps.length - 1], 1));
            }

            sb.append("}");

            return sb.toString();
        }
    }
}
