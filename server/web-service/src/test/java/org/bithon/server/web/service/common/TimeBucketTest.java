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

package org.bithon.server.web.service.common;

import org.bithon.server.web.service.common.bucket.TimeBucket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author frank.chen021@outlook.com
 * @date 24/11/21 7:11 pm
 */
public class TimeBucketTest {

    private long fromMinute(int minute) {
        return minute * 60L * 1000;
    }

    static TimeBucket getTimeBucket(long startTimestamp, long endTimestamp) {
        return TimeBucket.calculate(startTimestamp, endTimestamp, 60);
    }

    @Test
    public void testTimeBucketLessThan1Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 40_000;

        TimeBucket bucket = getTimeBucket(start, end);
        Assertions.assertEquals(1, bucket.getCount());
        Assertions.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket1Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(1);

        TimeBucket bucket = getTimeBucket(start, end);
        Assertions.assertEquals(1, bucket.getCount());
        Assertions.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket5Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(5);

        TimeBucket bucket = getTimeBucket(start, end);
        Assertions.assertEquals(5, bucket.getCount());
        Assertions.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket59Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(59);

        TimeBucket bucket = getTimeBucket(start, end);
        Assertions.assertEquals(59, bucket.getCount());
        Assertions.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket60Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(60);

        TimeBucket bucket = getTimeBucket(start, end);
        Assertions.assertEquals(60, bucket.getCount());
        Assertions.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket61Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(61);

        TimeBucket bucket = getTimeBucket(start, end);

        // After 60 minutes, the step is 12, so there should be 12 + 1 bucket in total
        Assertions.assertEquals(12 + 1, bucket.getCount());
        Assertions.assertEquals(300, bucket.getLength());
    }

    @Test
    public void testTimeBucket360Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(360);

        TimeBucket bucket = getTimeBucket(start, end);

        // 10 minute per bucket
        Assertions.assertEquals(60 * 10, bucket.getLength());

        // 36 buckets
        Assertions.assertEquals(36, bucket.getCount());
    }

    @Test
    public void testTimeBucket361Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(361);

        TimeBucket bucket = getTimeBucket(start, end);

        Assertions.assertEquals(600, bucket.getLength());
        Assertions.assertEquals(36 + 1, bucket.getCount());
    }
}
