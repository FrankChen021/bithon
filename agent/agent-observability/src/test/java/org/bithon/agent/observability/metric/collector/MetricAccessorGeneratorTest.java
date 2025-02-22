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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/11 21:38
 */
public class MetricAccessorGeneratorTest {

    public static class SampleData {
        public long field0 = 1;
        public long field1 = 2;
        public long field2 = 3;
    }

    @Test
    public void testClassGenerator() {
        SampleData sampleData = MetricAccessorGenerator.createInstantiator(SampleData.class).newInstance();

        IMetricAccessor metricAccessor = (IMetricAccessor) sampleData;

        Assertions.assertEquals(3, metricAccessor.getMetricCount());
        Assertions.assertEquals(1, metricAccessor.getMetricValue(0));
        Assertions.assertEquals(2, metricAccessor.getMetricValue(1));
        Assertions.assertEquals(3, metricAccessor.getMetricValue(2));

        Assertions.assertEquals(1, metricAccessor.getMetricValue("field0"));
        Assertions.assertEquals(2, metricAccessor.getMetricValue("field1"));
        Assertions.assertEquals(3, metricAccessor.getMetricValue("field2"));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> metricAccessor.getMetricValue("non_exists_field"));

        sampleData.field0 = 0;
        sampleData.field1 = 11;
        sampleData.field2 = 22;
        Assertions.assertEquals(0, metricAccessor.getMetricValue(0));
        Assertions.assertEquals(11, metricAccessor.getMetricValue(1));
        Assertions.assertEquals(22, metricAccessor.getMetricValue(2));
    }
}
