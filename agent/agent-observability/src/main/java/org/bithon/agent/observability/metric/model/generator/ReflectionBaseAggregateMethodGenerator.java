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

package org.bithon.agent.observability.metric.model.generator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/13 20:25
 */
public class ReflectionBaseAggregateMethodGenerator {

    public static <T> IAggregate<T> createAggregateFn(Class<T> clazz) {
        List<IAggregateFunction<T>> aggregateFunctions = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Sum.class)) {
                aggregateFunctions.add(new SumAggregateFunction<>(field));
            } else if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Max.class)) {
                aggregateFunctions.add(new MaxAggregateFunction<>(field));
            } else if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Min.class)) {
                aggregateFunctions.add(new MinAggregateFunction<>(field));
            } else if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Last.class)) {
                aggregateFunctions.add(new LastAggregateFunction<>(field));
            } else if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.First.class)) {
                aggregateFunctions.add(new FirstAggregateFunction<>(field));
            }
        }
        if (aggregateFunctions.isEmpty()) {
            throw new IllegalArgumentException("No aggregation annotation defined in class " + clazz.getName());
        }
        for (IAggregateFunction<T> aggregateFunction : aggregateFunctions) {
            aggregateFunction.getField().setAccessible(true);
        }
        return (prev, now) -> {
            for (IAggregateFunction<T> aggregateFunction : aggregateFunctions) {
                Field f = aggregateFunction.getField();
                try {
                    f.set(prev, aggregateFunction.aggregate(prev, now));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    interface IAggregateFunction<T> {
        Field getField();

        long aggregate(T prev, T now) throws Exception;
    }

    static class SumAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public SumAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return (long) field.get(prev) + (long) field.get(now);
        }
    }

    static class MinAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public MinAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return Math.min((long) field.get(prev), (long) field.get(now));
        }
    }

    static class MaxAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public MaxAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return Math.max((long) field.get(prev), (long) field.get(now));
        }
    }

    static class LastAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public LastAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return (long) field.get(now);

        }
    }

    static class FirstAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public FirstAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return (long) field.get(prev);
        }
    }
}
