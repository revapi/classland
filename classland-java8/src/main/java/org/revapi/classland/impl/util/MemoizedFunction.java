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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MemoizedFunction<T, R> implements Function<T, R> {
    private final @Nullable Function<T, R> action;
    protected Map<T, R> values;

    private MemoizedFunction(@Nullable Function<T, R> action) {
        this.action = action;
        values = new ConcurrentHashMap<>();
    }

    private static <T, R> MemoizedFunction<T, R> instantiate(@Nullable Function<T, R> action) {
        if (DEBUG) {
            return new Debug<>(action);
        } else {
            return new MemoizedFunction<>(action);
        }
    }

    public static <T, R> MemoizedFunction<T, R> memoize(Function<T, R> action) {
        if (action instanceof MemoizedFunction) {
            return (MemoizedFunction<T, R>) action;
        } else {
            return instantiate(action);
        }
    }

    @Override
    public R apply(T param) {
        return values.computeIfAbsent(param, __ -> action.apply(param));
    }

    public <U> MemoizedFunction<T, U> map(Function<R, U> action) {
        return instantiate(p -> {
            R val = apply(p);
            return action.apply(val);
        });
    }

    @Override
    public String toString() {
        return "Memoized1{size=" + values.size() + "}";
    }

    private static final class Debug<T, R> extends MemoizedFunction<T, R> {
        private final Throwable instantiationLocation;

        private Debug(@Nullable Function<T, R> action) {
            super(action);
            instantiationLocation = new Throwable();
        }

        @Override
        public R apply(T param) {
            try {
                return super.apply(param);
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
            return "MemoizedFunction{size=" + values.size() + " @ " + location() + "}";
        }
    }
}
