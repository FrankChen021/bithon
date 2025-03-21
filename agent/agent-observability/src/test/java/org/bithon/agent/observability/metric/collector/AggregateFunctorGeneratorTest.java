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

import org.bithon.agent.observability.metric.model.annotation.First;
import org.bithon.agent.observability.metric.model.annotation.Last;
import org.bithon.agent.observability.metric.model.annotation.Max;
import org.bithon.agent.observability.metric.model.annotation.Min;
import org.bithon.agent.observability.metric.model.annotation.Sum;
import org.bithon.agent.observability.metric.model.generator.AggregateFunctorGenerator;
import org.bithon.agent.observability.metric.model.generator.IAggregate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/11 21:34
 */
public class AggregateFunctorGeneratorTest {

    public static class SampleDataLong {
        @Sum
        public long sumField = 0;

        @Min
        public long minField = 0;

        @Max
        public long maxField = 0;

        @First
        public long firstField = 0;

        @Last
        public long lastField = 0;
    }

    @Test
    public void test() {
        // Create two instances
        SampleDataLong prev = new SampleDataLong();

        // Set test values
        prev.sumField = 100L;    // @Sum
        prev.minField = 200L;    // @Min
        prev.maxField = 300L;    // @Max
        prev.firstField = 400L;  // @First
        prev.lastField = 500L;   // @Last

        // Create aggregate function instance
        IAggregate<SampleDataLong> aggregateFunctor = AggregateFunctorGenerator.createAggregateFunctor(SampleDataLong.class);

        // Perform Aggregation
        {
            SampleDataLong now = new SampleDataLong();
            now.sumField = 150L;     // @Sum -> expected 250 (sum)
            now.minField = 100L;     // @Min -> expected 100 (min)
            now.maxField = 400L;     // @Max -> expected 400 (max)
            now.firstField = 500L;   // @First -> expected 400 (keep prev)
            now.lastField = 600L;    // @Last -> expected 600 (take now)
            aggregateFunctor.aggregate(prev, now);
            Assertions.assertEquals(250L, prev.sumField);
            Assertions.assertEquals(100L, prev.minField);
            Assertions.assertEquals(400L, prev.maxField);
            Assertions.assertEquals(400L, prev.firstField);
            Assertions.assertEquals(600L, prev.lastField);
        }
        {
            SampleDataLong now = new SampleDataLong();
            now.minField = Long.MAX_VALUE;
            now.maxField = Long.MIN_VALUE;
            aggregateFunctor.aggregate(prev, now);

            // The MIN and MAX should be the same as previous
            Assertions.assertEquals(100L, prev.minField);
            Assertions.assertEquals(400L, prev.maxField);
        }
    }
}
