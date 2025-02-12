/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.observability.metric.collector;

import org.bithon.agent.observability.metric.model.annotation.Sum;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/12 21:05
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class AggregateFunctorBenchmark {
    public static class SampleData {
        @Sum
        public long field0 = 1;
    }

    private AggregateFunctorGenerator.IAggregate<SampleData> generatedAggregator;
    private BiConsumer<SampleData, SampleData> codedAggregator;

    @Setup
    public void setup() throws Exception {
        // Generate the merger class
        Class<SampleData> aggregateFunctorClass = AggregateFunctorGenerator.createAggregateFunctor(SampleData.class);

        //noinspection unchecked,rawtypes
        generatedAggregator = (AggregateFunctorGenerator.IAggregate) aggregateFunctorClass.getDeclaredConstructor().newInstance();

        codedAggregator = (prev, next) -> prev.field0 += next.field0;
    }

    @Benchmark
    public void testGeneratedAggregator() {
        // Perform Aggregation
        // Set test values
        SampleData prev = new SampleData();
        prev.field0 = 100L;    // @Sum

        SampleData next = new SampleData();
        next.field0 = 200L;    // @Sum

        generatedAggregator.aggregate(prev, next);
    }

    @Benchmark
    public void testCodedAggregator() {
        // Perform Aggregation
        // Set test values
        SampleData prev = new SampleData();
        prev.field0 = 100L;    // @Sum

        SampleData next = new SampleData();
        next.field0 = 200L;    // @Sum

        codedAggregator.accept(prev, next);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(AggregateFunctorBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(1)
            .measurementIterations(5)
            .build();

        new Runner(opt).run();
    }
}
