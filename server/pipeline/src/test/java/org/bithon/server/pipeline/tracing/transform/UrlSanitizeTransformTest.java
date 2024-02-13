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

package org.bithon.server.pipeline.tracing.transform;

import com.google.common.collect.ImmutableMap;
import org.bithon.server.pipeline.tracing.transform.sanitization.UrlSanitizeTransformer;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.tracing.TraceSpan;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author Frank Chen
 * @date 13/2/24 1:53 pm
 */
public class UrlSanitizeTransformTest {
    @Test
    public void test() {
        ITransformer transformer = new UrlSanitizeTransformer(null, ImmutableMap.of("http.url", "password",
                                                                                    "url", "password"));

        TraceSpan span = TraceSpan.builder()
                                  .tags(new HashMap<>(ImmutableMap.of("http.url", "/?database=default&user=1&password=123",
                                                                      "url", "/?database=default&user=1&password=123"))).build();
        transformer.transform(span);

        Assert.assertEquals("/?database=default&user=1&password=*HIDDEN*", span.getTag("http.url"));
        Assert.assertEquals("/?database=default&user=1&password=*HIDDEN*", span.getTag("url"));
    }
}
