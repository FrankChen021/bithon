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

package org.bithon.server.pipeline.tracing.mapping;

import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 10/12/21 4:25 PM
 */
public class URIParameterExtractorTest {
    @Test
    public void testUpstreamTraceId() {
        TraceSpan span = TraceSpan.builder()
                                  .traceId("1")
                                  .startTime(System.currentTimeMillis())
                                  .kind("SERVER")
                                  .tags(ImmutableMap.of("upstreamTraceId",
                                                        "123456",
                                                        "status",
                                                        "200")).build();

        TraceIdMappingBatchExtractor extractor = TraceIdMappingBatchExtractor.create(new URIParameterExtractor(ImmutableMap.of("uri", ImmutableMap.of("0", "query_id"))));
        List<TraceIdMapping> mappings = extractor.extract(Collections.singletonList(span));
        Assert.assertEquals(2, mappings.size());

        // Trace Id
        Assert.assertEquals("1", mappings.get(0).getUserId());
        Assert.assertEquals("1", mappings.get(0).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(0).getTimestamp());

        // User Id
        Assert.assertEquals("123456", mappings.get(1).getUserId());
        Assert.assertEquals("1", mappings.get(1).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(1).getTimestamp());
    }

    @Test
    public void testAbsoluteURI() {
        TraceSpan span = new TraceSpan();
        span.setTraceId("1");
        span.setStartTime(System.currentTimeMillis());
        span.setTags(ImmutableMap.of("uri",
                                     "http://localhost:26029/?query=SELECT+1&query_id=C0A802F1fbe7b9768c2949738cbb5ce383e21d5f",
                                     "status",
                                     "200"));

        TraceIdMappingBatchExtractor extractor = TraceIdMappingBatchExtractor.create(new URIParameterExtractor(ImmutableMap.of("uri", ImmutableMap.of("0", "query_id"))));
        List<TraceIdMapping> mappings = extractor.extract(Collections.singletonList(span));

        Assert.assertEquals(2, mappings.size());

        // Trace Id
        Assert.assertEquals("1", mappings.get(0).getUserId());
        Assert.assertEquals("1", mappings.get(0).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(0).getTimestamp());

        // User Id
        Assert.assertEquals("C0A802F1fbe7b9768c2949738cbb5ce383e21d5f", mappings.get(1).getUserId());
        Assert.assertEquals("1", mappings.get(1).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(1).getTimestamp());
    }

    @Test
    public void testRelativeURI() {
        TraceSpan span = new TraceSpan();
        span.setTraceId("1");
        span.setStartTime(System.currentTimeMillis());
        span.setTags(ImmutableMap.of("uri",
                                     "/?query=SELECT+1&query_id=C0A802F1fbe7b9768c2949738cbb5ce383e21d5f",
                                     "status",
                                     "200"));

        TraceIdMappingBatchExtractor extractor = TraceIdMappingBatchExtractor.create(new URIParameterExtractor(ImmutableMap.of("uri", ImmutableMap.of("0", "query_id"))));
        List<TraceIdMapping> mappings = extractor.extract(Collections.singletonList(span));

        Assert.assertEquals(2, mappings.size());
        // Trace Id
        Assert.assertEquals("1", mappings.get(0).getUserId());
        Assert.assertEquals("1", mappings.get(0).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(0).getTimestamp());

        // User Id
        Assert.assertEquals("C0A802F1fbe7b9768c2949738cbb5ce383e21d5f", mappings.get(1).getUserId());
        Assert.assertEquals("1", mappings.get(1).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(1).getTimestamp());
    }

    @Test
    public void testParameterExtraction() {
        TraceSpan span = new TraceSpan();
        span.setTraceId("1");
        span.setStartTime(System.currentTimeMillis());
        span.setTags(ImmutableMap.of("uri", "/?query=SELECT+1&query_id====", "status", "200"));

        TraceIdMappingBatchExtractor extractor = TraceIdMappingBatchExtractor.create(new URIParameterExtractor(ImmutableMap.of("uri", ImmutableMap.of("0", "query_id"))));
        List<TraceIdMapping> mappings = extractor.extract(Collections.singletonList(span));
        Assert.assertEquals(2, mappings.size());

        // Trace Id
        Assert.assertEquals("1", mappings.get(0).getUserId());
        Assert.assertEquals("1", mappings.get(0).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(0).getTimestamp());

        // User Id
        Assert.assertEquals("===", mappings.get(1).getUserId());
        Assert.assertEquals("1", mappings.get(1).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(1).getTimestamp());
    }

    @Test
    public void test_Deduplication() {
        TraceSpan span = new TraceSpan();
        span.setTraceId("1");
        span.setStartTime(System.currentTimeMillis());
        span.setTags(ImmutableMap.of("uri", "/?query=SELECT+1&query_id=uid", "status", "200"));

        TraceSpan span2 = new TraceSpan();
        span2.setTraceId("1");
        span2.setStartTime(System.currentTimeMillis());
        span2.setTags(ImmutableMap.of("uri", "/?query=SELECT+1&query_id=uid", "status", "200"));

        TraceIdMappingBatchExtractor extractor = TraceIdMappingBatchExtractor.create(new URIParameterExtractor(ImmutableMap.of("uri", ImmutableMap.of("0", "query_id"))));
        List<TraceIdMapping> mappings = extractor.extract(Collections.singletonList(span));
        Assert.assertEquals(2, mappings.size());

        // Trace Id
        Assert.assertEquals("1", mappings.get(0).getUserId());
        Assert.assertEquals("1", mappings.get(0).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(0).getTimestamp());

        // User Id
        Assert.assertEquals("uid", mappings.get(1).getUserId());
        Assert.assertEquals("1", mappings.get(1).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(1).getTimestamp());
    }
}
