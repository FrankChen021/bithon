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

package org.bithon.server.pipeline.tracing.index;

import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.index.TagIndex;
import org.bithon.server.storage.tracing.index.TagIndexConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 3/3/22 2:00 PM
 */
public class TagIndexBuilderTest {

    private TraceStorageConfig newStorageConfig(TagIndexConfig tagIndexConfig) {
        TraceStorageConfig config = new TraceStorageConfig();
        config.setIndexes(tagIndexConfig);
        return config;
    }

    @Test
    public void testEmptyConfig() {
        TagIndexGenerator builder = new TagIndexGenerator(newStorageConfig(new TagIndexConfig()));
        Collection<TagIndex> indexes = builder.generate(Collections.singletonList(TraceSpan.builder()
                                                                                           .startTime(1000)
                                                                                           .appName("bithon-test")
                                                                                           .build()));

        Assert.assertTrue(indexes.isEmpty());
    }

    @Test
    public void testTagNotMatched() {
        TagIndexConfig indexConfig = new TagIndexConfig();
        indexConfig.setMap(new LinkedHashMap<>(ImmutableMap.of("status", 1)));

        TagIndexGenerator builder = new TagIndexGenerator(newStorageConfig(indexConfig));
        Collection<TagIndex> indexes = builder.generate(Collections.singletonList(TraceSpan.builder()
                                                                                           .startTime(1000)
                                                                                           .appName("bithon-test")
                                                                                           .tags(ImmutableMap.of("status1", "200"))
                                                                                           .build()));

        Assert.assertTrue(indexes.isEmpty());
    }

    @Test
    public void testTagMatched() {
        TagIndexConfig indexConfig = new TagIndexConfig();
        indexConfig.setMap(new LinkedHashMap<>(ImmutableMap.of("status", 1)));

        TagIndexGenerator builder = new TagIndexGenerator(newStorageConfig(indexConfig));
        List<TagIndex> indexes = builder.generate(Collections.singletonList(TraceSpan.builder()
                                                                                     .startTime(1000)
                                                                                     .appName("bithon-test")
                                                                                     .traceId("trace-id-123")
                                                                                     .tags(ImmutableMap.of("status", "200"))
                                                                                     .build()));

        Assert.assertEquals(1, indexes.size());
        Assert.assertEquals("trace-id-123", indexes.get(0).getTraceId());
        Assert.assertEquals("status", indexes.get(0).getName());
        Assert.assertEquals("200", indexes.get(0).getValue());
        Assert.assertEquals(1, indexes.get(0).getTimestamp());
    }
}
