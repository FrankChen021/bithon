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

package org.bithon.server.commons.time;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Duration;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author Frank Chen
 * @date 18/8/22 3:49 pm
 */
public class TimeSpanTest {

    @Test
    public void testBefore() {

        TimeSpan span = new TimeSpan(System.currentTimeMillis());
        Assertions.assertEquals(span.getMilliseconds() - 10, span.before(10, TimeUnit.MILLISECONDS).getMilliseconds());

        Assertions.assertEquals(span.getMilliseconds() - 1000, span.before(1, TimeUnit.SECONDS).getMilliseconds());

        Assertions.assertEquals(span.getMilliseconds() - 60_000, span.before(1, TimeUnit.MINUTES).getMilliseconds());

        Assertions.assertEquals(span.getMilliseconds() - 3600_000, span.before(1, TimeUnit.HOURS).getMilliseconds());

        Assertions.assertEquals(span.getMilliseconds() - 24 * 3600_000, span.before(1, TimeUnit.DAYS).getMilliseconds());
    }

    @Test
    public void testFloorAndCeil_Round2Minute() throws ParseException {
        TimeZone tz = TimeZone.getTimeZone("UTC");

        // Round to minute
        {
            TimeSpan span = TimeSpan.fromString("2022-05-15 12:38:43", "yyyy-MM-dd HH:mm:ss", tz);

            Assertions.assertEquals("2022-05-15 12:38:43", span.format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 12:38:00", span.floor(Duration.ofMinutes(1)).format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 12:39:00", span.ceil(Duration.ofMinutes(1)).format("yyyy-MM-dd HH:mm:ss", tz));
        }

        // Boundary testing
        {
            TimeSpan span = TimeSpan.fromString("2022-05-15 12:38:00", "yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("UTC"));
            Assertions.assertEquals("2022-05-15 12:38:00", span.format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 12:38:00", span.floor(Duration.ofMinutes(1)).format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 12:39:00", span.ceil(Duration.ofMinutes(1)).format("yyyy-MM-dd HH:mm:ss", tz));
        }
    }

    @Test
    public void testFloorAndCeil_Round2Hour() throws ParseException {
        TimeZone tz = TimeZone.getTimeZone("UTC");

        {
            TimeSpan span = TimeSpan.fromString("2022-05-15 12:38:43", "yyyy-MM-dd HH:mm:ss", tz);

            Assertions.assertEquals("2022-05-15 12:38:43", span.format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 12:00:00", span.floor(Duration.ofHours(1)).format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 13:00:00", span.ceil(Duration.ofHours(1)).format("yyyy-MM-dd HH:mm:ss", tz));
        }

        // Boundary testing
        {
            TimeSpan span = TimeSpan.fromString("2022-05-15 12:00:00", "yyyy-MM-dd HH:mm:ss", tz);
            Assertions.assertEquals("2022-05-15 12:00:00", span.format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 12:00:00", span.floor(Duration.ofHours(1)).format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 13:00:00", span.ceil(Duration.ofHours(1)).format("yyyy-MM-dd HH:mm:ss", tz));
        }
    }

    @Test
    public void testFloorAndCeil_RoundDay() throws ParseException {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        {
            // from a UTC timezone
            TimeSpan span = TimeSpan.fromString("2022-05-15 12:38:43", "yyyy-MM-dd HH:mm:ss", tz);

            Assertions.assertEquals("2022-05-15 12:38:43", span.format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 00:00:00", span.floor(Duration.ofDays(1)).format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-16 00:00:00", span.ceil(Duration.ofDays(1)).format("yyyy-MM-dd HH:mm:ss", tz));
        }

        // Boundary testing
        {
            TimeSpan span = TimeSpan.fromString("2022-05-15 00:00:00", "yyyy-MM-dd HH:mm:ss", tz);
            Assertions.assertEquals("2022-05-15 00:00:00", span.format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-15 00:00:00", span.floor(Duration.ofDays(1)).format("yyyy-MM-dd HH:mm:ss", tz));
            Assertions.assertEquals("2022-05-16 00:00:00", span.ceil(Duration.ofDays(1)).format("yyyy-MM-dd HH:mm:ss", tz));
        }
    }

    @Test
    public void testJacksonDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Test ISO8601 string deserialization
        String isoJson = "\"2022-05-15T12:38:43.000Z\"";
        TimeSpan isoTimeSpan = mapper.readValue(isoJson, TimeSpan.class);
        Assertions.assertEquals("2022-05-15 12:38:43", isoTimeSpan.format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("UTC")));

        // Test milliseconds deserialization
        long millis = TimeSpan.fromISO8601("2022-05-15T12:38:43.000Z").getMilliseconds();
        String millisJson = String.valueOf(millis);
        TimeSpan millisTimeSpan = mapper.readValue(millisJson, TimeSpan.class);
        Assertions.assertEquals("2022-05-15 12:38:43", millisTimeSpan.format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("UTC")));

        // Test that both methods produce equal TimeSpan objects
        Assertions.assertEquals(isoTimeSpan, millisTimeSpan);
    }

    @Test
    public void testJacksonSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Create a TimeSpan with a known time
        TimeSpan timeSpan = TimeSpan.fromISO8601("2022-05-15T12:38:43.000Z");

        // Serialize to JSON
        String json = mapper.writeValueAsString(timeSpan);

        // Verify serialized to expected ISO8601 format
        Assertions.assertEquals("\"2022-05-15T12:38:43.000Z\"", json);

        // Verify round-trip serialization/deserialization
        TimeSpan deserializedTimeSpan = mapper.readValue(json, TimeSpan.class);
        Assertions.assertEquals(timeSpan, deserializedTimeSpan);

        // Verify milliseconds remain unchanged after round-trip
        Assertions.assertEquals(timeSpan.getMilliseconds(), deserializedTimeSpan.getMilliseconds());
    }
}
