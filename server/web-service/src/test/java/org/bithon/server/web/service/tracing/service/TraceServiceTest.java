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

package org.bithon.server.web.service.tracing.service;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Frank Chen
 * @date 24/11/21 7:11 pm
 */
public class TraceServiceTest {

    private long fromSecond(int second) {
        return second * 1000L;
    }

    private long fromMinute(int minute) {
        return minute * 60L * 1000;
    }

    @Test
    public void testTimeBucketLessThan1Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromSecond(40);

        TraceService.Bucket bucket = TraceService.getTimeBucket(start, end);
        Assert.assertEquals(1, bucket.getNums());
        Assert.assertEquals(60, bucket.getLength());

    }

    @Test
    public void testTimeBucket1Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(1);

        TraceService.Bucket bucket = TraceService.getTimeBucket(start, end);
        Assert.assertEquals(1, bucket.getNums());
        Assert.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket5Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(5);

        TraceService.Bucket bucket = TraceService.getTimeBucket(start, end);
        Assert.assertEquals(5, bucket.getNums());
        Assert.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket59Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(59);

        TraceService.Bucket bucket = TraceService.getTimeBucket(start, end);
        Assert.assertEquals(59, bucket.getNums());
        Assert.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket60Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(60);

        TraceService.Bucket bucket = TraceService.getTimeBucket(start, end);
        Assert.assertEquals(60, bucket.getNums());
        Assert.assertEquals(60, bucket.getLength());
    }

    @Test
    public void testTimeBucket61Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(61);

        TraceService.Bucket bucket = TraceService.getTimeBucket(start, end);
        Assert.assertEquals(31, bucket.getNums());
        Assert.assertEquals(120, bucket.getLength());
    }

    @Test
    public void testTimeBucket360Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(360);

        TraceService.Bucket bucket = TraceService.getTimeBucket(start, end);
        Assert.assertEquals(60, bucket.getNums());
        Assert.assertEquals(360, bucket.getLength());
    }

    @Test
    public void testTimeBucket361Minute() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + fromMinute(361);

        TraceService.Bucket bucket = TraceService.getTimeBucket(start, end);
        Assert.assertEquals(31, bucket.getNums());
        Assert.assertEquals(720, bucket.getLength());
    }
}
