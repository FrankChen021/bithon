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
import org.bithon.agent.observability.metric.model.generator.AggregateFunctorGenerator;
import org.bithon.agent.observability.metric.model.generator.IAggregate;
import org.bithon.agent.observability.metric.model.generator.ReflectionBaseAggregateMethodGenerator;
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
 * The benchmark below running on a MacBookPro M1 shows
 * that the generated aggregator has the same performance as the coded one,
 * while it improves performance by 25x over the traditional reflection based one.
 * <pre>
 * Benchmark                                                      Mode  Cnt   Score   Error  Units
 * AggregateFunctorBenchmark.aggregate_CodedAggregator            avgt    5   0.674 ± 0.088  ns/op
 * AggregateFunctorBenchmark.aggregate_GeneratedAggregator        avgt    5   0.667 ± 0.013  ns/op
 * AggregateFunctorBenchmark.aggregate_ReflectionCodedAggregator  avgt    5  15.880 ± 0.515  ns/op
 * </pre>
 *
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

    private IAggregate<SampleData> generatedAggregator;
    private BiConsumer<SampleData, SampleData> codedAggregator;
    private IAggregate<SampleData> reflectionBasedAggregator;

    @Setup
    public void setup() {
        generatedAggregator = AggregateFunctorGenerator.createAggregateFunctor(SampleData.class);

        codedAggregator = (prev, next) -> prev.field0 += next.field0;

        reflectionBasedAggregator = ReflectionBaseAggregateMethodGenerator.createAggregateFn(SampleData.class);
    }

    @Benchmark
    public void aggregate_GeneratedAggregator() {
        // Perform Aggregation
        // Set test values
        SampleData prev = new SampleData();
        prev.field0 = 100L;    // @Sum

        SampleData next = new SampleData();
        next.field0 = 200L;    // @Sum

        generatedAggregator.aggregate(prev, next);
    }

    @Benchmark
    public void aggregate_CodedAggregator() {
        // Perform Aggregation
        // Set test values
        SampleData prev = new SampleData();
        prev.field0 = 100L;    // @Sum

        SampleData next = new SampleData();
        next.field0 = 200L;    // @Sum

        codedAggregator.accept(prev, next);
    }

    @Benchmark
    public void aggregate_ReflectionCodedAggregator() {
        // Perform Aggregation
        // Set test values
        SampleData prev = new SampleData();
        prev.field0 = 100L;    // @Sum

        SampleData next = new SampleData();
        next.field0 = 200L;    // @Sum

        reflectionBasedAggregator.aggregate(prev, next);
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
