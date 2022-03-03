package org.bithon.server.tracing.index;

import com.google.common.collect.ImmutableMap;
import org.bithon.server.tracing.TraceConfig;
import org.bithon.server.tracing.sink.TraceSpan;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 3/3/22 2:00 PM
 */
public class TagIndexBuilderTest {

    @Test
    public void testEmptyConfig() {
        TraceConfig config = new TraceConfig();
        TagIndexBuilder builder = new TagIndexBuilder(config);
        Collection<TagIndex> indexes = builder.build(Collections.singletonList(TraceSpan.builder()
                                                                                        .startTime(1000)
                                                                                        .appName("bithon-test")
                                                                                        .build()));

        Assert.assertTrue(indexes.isEmpty());
    }

    @Test
    public void testTagNotMatched() {
        TagIndexConfig indexConfig = new TagIndexConfig();
        indexConfig.setNames(Collections.singletonList("status"));
        TraceConfig config = new TraceConfig();
        config.setTagIndex(indexConfig);

        TagIndexBuilder builder = new TagIndexBuilder(config);
        Collection<TagIndex> indexes = builder.build(Collections.singletonList(TraceSpan.builder()
                                                                                        .startTime(1000)
                                                                                        .appName("bithon-test")
                                                                                        .tags(ImmutableMap.of("status1", "200"))
                                                                                        .build()));

        Assert.assertTrue(indexes.isEmpty());
    }

    @Test
    public void testTagMatched() {
        TagIndexConfig indexConfig = new TagIndexConfig();
        indexConfig.setNames(Collections.singletonList("status"));
        TraceConfig config = new TraceConfig();
        config.setTagIndex(indexConfig);

        TagIndexBuilder builder = new TagIndexBuilder(config);
        List<TagIndex> indexes = builder.build(Collections.singletonList(TraceSpan.builder()
                                                                                  .startTime(1000)
                                                                                  .appName("bithon-test")
                                                                                  .traceId("trace-id-123")
                                                                                  .tags(ImmutableMap.of("status", "200"))
                                                                                  .build()));

        Assert.assertEquals(1, indexes.size());
        Assert.assertEquals("bithon-test", indexes.get(0).getApplication());
        Assert.assertEquals("trace-id-123", indexes.get(0).getTraceId());
        Assert.assertEquals("status", indexes.get(0).getName());
        Assert.assertEquals("200", indexes.get(0).getValue());
        Assert.assertEquals(1, indexes.get(0).getTimestamp());
    }
}
