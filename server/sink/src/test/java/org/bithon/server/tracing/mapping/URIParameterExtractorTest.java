package org.bithon.server.tracing.mapping;

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

import com.google.common.collect.ImmutableMap;
import org.bithon.server.tracing.sink.TraceSpan;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author Frank Chen
 * @date 10/12/21 4:25 PM
 */
public class URIParameterExtractorTest {
    @Test
    public void testAbsoluteURI() {
        TraceSpan span = new TraceSpan();
        span.setTraceId("1");
        span.setStartTime(System.currentTimeMillis());
        span.setTags(ImmutableMap.of("uri",
                                     "http://localhost:26029/?query=SELECT+1&query_id=C0A802F1fbe7b9768c2949738cbb5ce383e21d5f",
                                     "status",
                                     "200"));

        Function<Collection<TraceSpan>, List<TraceMapping>> extractor = TraceMappingFactory.create(new URIParameterExtractor(
            Collections.singletonList("query_id")));
        List<TraceMapping> mappings = extractor.apply(Collections.singletonList(span));
        Assert.assertEquals(1, mappings.size());
        Assert.assertEquals("C0A802F1fbe7b9768c2949738cbb5ce383e21d5f", mappings.get(0).getUserId());
        Assert.assertEquals("1", mappings.get(0).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(0).getTimestamp());
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

        Function<Collection<TraceSpan>, List<TraceMapping>> extractor = TraceMappingFactory.create(new URIParameterExtractor(
            Collections.singletonList("query_id")));
        List<TraceMapping> mappings = extractor.apply(Collections.singletonList(span));
        Assert.assertEquals(1, mappings.size());
        Assert.assertEquals("C0A802F1fbe7b9768c2949738cbb5ce383e21d5f", mappings.get(0).getUserId());
        Assert.assertEquals("1", mappings.get(0).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(0).getTimestamp());
    }

    @Test
    public void testParameterExtraction() {
        TraceSpan span = new TraceSpan();
        span.setTraceId("1");
        span.setStartTime(System.currentTimeMillis());
        span.setTags(ImmutableMap.of("uri", "/?query=SELECT+1&query_id====", "status", "200"));

        Function<Collection<TraceSpan>, List<TraceMapping>> extractor = TraceMappingFactory.create(new URIParameterExtractor(
            Collections.singletonList("query_id")));
        List<TraceMapping> mappings = extractor.apply(Collections.singletonList(span));
        Assert.assertEquals(1, mappings.size());
        Assert.assertEquals("===", mappings.get(0).getUserId());
        Assert.assertEquals("1", mappings.get(0).getTraceId());
        Assert.assertEquals(span.getStartTime() / 1000L, mappings.get(0).getTimestamp());
    }
}
