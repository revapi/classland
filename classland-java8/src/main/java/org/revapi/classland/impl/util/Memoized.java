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

import org.apache.logging.log4j.LogManager;

public class Memoized<T> implements Supplier<T> {
    private static final boolean DEBUG = LogManager.getLogger(Memoized.class).isDebugEnabled();

    private @Nullable Supplier<T> action;
    protected volatile boolean obtained;
    protected T value;

    private Memoized(@Nullable Supplier<T> action) {
        this.action = action;
    }

    private static <T> Memoized<T> instantiate(@Nullable Supplier<T> action) {
        if (DEBUG) {
            return new Debug<>(action);
        } else {
            return new Memoized<>(action);
        }
    }

    public static <T> Memoized<T> memoize(Supplier<T> action) {
        if (action instanceof Memoized) {
            return (Memoized<T>) action;
        } else {
            return instantiate(action);
        }
    }

    public static <T> Memoized<T> obtained(T value) {
        Memoized<T> ret = instantiate(null);
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

    public <U> Memoized<U> map(Function<T, U> action) {
        return instantiate(() -> {
            T val = get();
            return action.apply(val);
        });
    }

    @Override
    public String toString() {
        return "Memoized{" + (obtained ? ("value=" + value) : "<pending>") + "}";
    }

    private static final class Debug<T> extends Memoized<T> {
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

        private String location() {
            StackTraceElement el = instantiationLocation.getStackTrace()[4];
            return el.getClassName() + "." + el.getMethodName() + "(" + el.getFileName() + ":" + el.getLineNumber()
                    + ")";
        }

        @Override
        public String toString() {
            return "Memoized{" + (obtained ? ("value=" + value) : "<pending>") + " @ " + location() + "}";
        }
    }
}
