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

import org.bithon.agent.observability.metric.model.IMetricAccessor;
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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/12 20:47
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class MetricAccessorBenchmark {
    public static class SampleData {
        public long field0 = 1;
        public long field1 = 2;
        public long field2 = 3;
    }

    static class ReflectionBasedMetricAccessor implements IMetricAccessor {
        private final Object metricObject;
        public List<Field> metricFields;

        public ReflectionBasedMetricAccessor(Object metricObject) {
            this.metricObject = metricObject;
            this.metricFields = Arrays.stream(metricObject.getClass().getDeclaredFields())
                                      .collect(Collectors.toList());
        }

        @Override
        public long getMetricValue(int index) {
            if (index >= this.metricFields.size()) {
                return Long.MAX_VALUE;
            }

            try {
                return (long) this.metricFields.get(index).get(metricObject);
            } catch (IllegalAccessException e) {
                return Long.MAX_VALUE;
            }
        }

        @Override
        public int getMetricCount() {
            return this.metricFields.size();
        }
    }

    private IMetricAccessor generatedAccessor;
    private ReflectionBasedMetricAccessor reflectionAccessor;

    @Setup
    public void setup() {
        SampleData sampleData = MetricAccessorGenerator.createInstantiator(SampleData.class).newInstance();
        generatedAccessor = (IMetricAccessor) sampleData;
        reflectionAccessor = new ReflectionBasedMetricAccessor(sampleData);
    }

    @Benchmark
    public long testGeneratedAccessor() {
        return generatedAccessor.getMetricValue(1);
    }

    @Benchmark
    public long testReflectionAccessor() {
        return reflectionAccessor.getMetricValue(1);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(MetricAccessorBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(1)
            .measurementIterations(5)
            .build();

        new Runner(opt).run();
    }
}
