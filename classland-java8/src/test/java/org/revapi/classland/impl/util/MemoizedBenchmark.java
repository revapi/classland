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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

public class MemoizedBenchmark {

    @State(Scope.Thread)
    public static class MemoizedPayloadHolder {
        MemoizedValue<Double> work = MemoizedValue.memoize(MemoizedBenchmark::work);
        MemoizedValue<Double> noWork = MemoizedValue.memoize(MemoizedBenchmark::noWork);
    }

    @Benchmark
    public void work(Blackhole blackhole) {
        blackhole.consume(work());
    }

    @Benchmark
    public void memoizedWork(MemoizedPayloadHolder state, Blackhole blackhole) {
        blackhole.consume(state.work.get());
    }

    @Benchmark
    public void noWork(Blackhole blackhole) {
        blackhole.consume(noWork());
    }

    @Benchmark
    public void memoizedNoWork(MemoizedPayloadHolder state, Blackhole blackhole) {
        blackhole.consume(state.noWork.get());
    }

    private static double work() {
        return Math.random();
    }

    private static double noWork() {
        return 0.0d;
    }
}
