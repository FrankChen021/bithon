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

package org.bithon.server.pipeline.common.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.datasource.input.InputRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/10/25
 */
public class DropTransformerTest {

    @Test
    public void testDropWithSimpleExpression() throws JsonProcessingException {
        // Test a simple drop expression
        DropTransformer transformer = new DropTransformer("age > 100", null);

        // deserialize from JSON to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        // Test case 1: age > 100 - should be dropped
        InputRow dropRow = new InputRow(new HashMap<>(ImmutableMap.of("age", 150)));
        TransformResult dropResult = newTransformer.transform(dropRow);
        Assertions.assertEquals(TransformResult.DROP, dropResult, "Row with age > 100 should be dropped");

        // Test case 2: age <= 100 - should NOT be dropped
        InputRow keepRow = new InputRow(new HashMap<>(ImmutableMap.of("age", 50)));
        TransformResult keepResult = newTransformer.transform(keepRow);
        Assertions.assertEquals(TransformResult.CONTINUE, keepResult, "Row with age <= 100 should not be dropped");
    }

    @Test
    public void testCurrentMillisecondsFunction() throws JsonProcessingException {
        // Test that currentMilliseconds() function returns a reasonable value
        // This expression checks if current time is less than a past date (September 2001)
        // The expression will be false, so rows should NOT be dropped
        DropTransformer transformer = new DropTransformer("currentMilliseconds() < 1000000000000", null);

        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        // The Current time should be greater than 1000000000000 (September 2001)
        // So the expression should be false, and the row should NOT be dropped
        InputRow row = new InputRow(new HashMap<>());
        TransformResult result = newTransformer.transform(row);
        Assertions.assertEquals(TransformResult.CONTINUE, result, "currentMilliseconds() should return current time > Sept 2001");
    }

    @Test
    public void testDropWithTimestampValidation() throws JsonProcessingException {
        // Test the expression pattern from the issue description
        // We test using absolute time comparisons to avoid schema issues
        // 7 days in milliseconds = 7 * 24 * 60 * 60 * 1000 = 604800000
        long sevenDaysInMs = 7L * 24 * 60 * 60 * 1000;
        long currentTime = System.currentTimeMillis();
        long minAcceptableTime = currentTime - sevenDaysInMs;
        long maxAcceptableTime = currentTime + sevenDaysInMs;
        
        // Expression: drop if timestamp is outside the acceptable range
        String expr = "timestamp < " + minAcceptableTime + " OR timestamp > " + maxAcceptableTime;
        
        DropTransformer transformer = new DropTransformer(expr, null);

        // deserialize from JSON to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        // Test case 1: Valid timestamp (current time) - should NOT be dropped
        InputRow validRow = new InputRow(new HashMap<>(ImmutableMap.of("timestamp", currentTime)));
        TransformResult validResult = newTransformer.transform(validRow);
        Assertions.assertEquals(TransformResult.CONTINUE, validResult, "Current timestamp should not be dropped");

        // Test case 2: Timestamp too far in the past (1970) - should be dropped
        long invalidPastTime = 0L; // Unix epoch
        InputRow invalidPastRow = new InputRow(new HashMap<>(ImmutableMap.of("timestamp", invalidPastTime)));
        TransformResult pastResult = newTransformer.transform(invalidPastRow);
        Assertions.assertEquals(TransformResult.DROP, pastResult, "Timestamp from 1970 should be dropped");

        // Test case 3: Timestamp too far in the future - should be dropped
        long invalidFutureTime = 10_000_000_000_000L; // Far future timestamp (~November 2286)
        InputRow invalidFutureRow = new InputRow(new HashMap<>(ImmutableMap.of("timestamp", invalidFutureTime)));
        TransformResult futureResult = newTransformer.transform(invalidFutureRow);
        Assertions.assertEquals(TransformResult.DROP, futureResult, "Timestamp from far future should be dropped");

        // Test case 4: Timestamp 6 days in the past - should NOT be dropped (within acceptable range)
        long sixDaysAgo = currentTime - (6L * 24 * 60 * 60 * 1000);
        InputRow sixDaysAgoRow = new InputRow(new HashMap<>(ImmutableMap.of("timestamp", sixDaysAgo)));
        TransformResult sixDaysAgoResult = newTransformer.transform(sixDaysAgoRow);
        Assertions.assertEquals(TransformResult.CONTINUE, sixDaysAgoResult, "Timestamp from 6 days ago should not be dropped");

        // Test case 5: Timestamp 6 days in the future - should NOT be dropped (within acceptable range)
        long sixDaysFuture = currentTime + (6L * 24 * 60 * 60 * 1000);
        InputRow sixDaysFutureRow = new InputRow(new HashMap<>(ImmutableMap.of("timestamp", sixDaysFuture)));
        TransformResult sixDaysFutureResult = newTransformer.transform(sixDaysFutureRow);
        Assertions.assertEquals(TransformResult.CONTINUE, sixDaysFutureResult, "Timestamp from 6 days in future should not be dropped");
    }
}
