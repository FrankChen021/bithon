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

package org.bithon.server.metric.expression.pipeline.step;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.plan.physical.PipelineQueryResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

/**
 * @author frank.chen021@outlook.com
 * @date 31/5/25 6:06 pm
 */
public class LiteralQueryStepTest {
    @Test
    public void testScalar() throws ExecutionException, InterruptedException {
        LiteralQueryStep step = new LiteralQueryStep(LiteralExpression.ofLong(123L));
        PipelineQueryResult result = step.execute().get();
        Assertions.assertEquals(IDataType.LONG, result.getTable().getColumn("value").getDataType());
        Assertions.assertEquals(1, result.getTable().getColumn("value").size());
        Assertions.assertEquals(123L, result.getTable().getColumn("value").getLong(0));
    }

    @Test
    public void testVector() throws ExecutionException, InterruptedException {
        LiteralQueryStep step = new LiteralQueryStep(LiteralExpression.ofLong(123L),
                                                     Interval.of(TimeSpan.fromISO8601("2024-01-01T00:00:00+00:00"),
                                                                 TimeSpan.fromISO8601("2024-01-01T01:00:10+00:00"),
                                                                 Duration.ofMinutes(1),
                                                                 null)
        );
        PipelineQueryResult result = step.execute().get();
        Assertions.assertEquals(IDataType.LONG, result.getTable().getColumn("value").getDataType());

        // 1 hour length, 1 minute interval, so 60 rows
        Assertions.assertEquals(60, result.getTable().getColumn("value").size());
        for (int i = 0; i < 60; i++) {
            Assertions.assertEquals(123L, result.getTable().getColumn("value").getLong(i));
        }
    }
}
