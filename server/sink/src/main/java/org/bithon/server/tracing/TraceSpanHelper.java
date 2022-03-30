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

package org.bithon.server.tracing;

import org.bithon.server.common.service.UriNormalizer;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 30/3/22 10:50 PM
 */
public class TraceSpanHelper {

    /**
     * flatten some properties in tags.
     * SHOULD be called AFTER all properties have been set
     */
    public static void flatten(TraceSpan span, UriNormalizer uriNormalizer) {
        Map<String, String> tags = span.getTags();
        if (!"SERVER".equals(span.getKind()) || CollectionUtils.isEmpty(tags)) {
            return;
        }

        String status = tags.getOrDefault("http.status", "");
        if ("".equals(status)) {
            // compatibility
            status = tags.getOrDefault("status", "");
        }
        span.setStatus(status);

        String uri = tags.getOrDefault("http.uri", "");
        if ("".equals(uri)) {
            // compatibility
            uri = tags.getOrDefault("uri", "");
        }
        span.setNormalizeUri(uriNormalizer.normalize(span.getAppName(), uri).getUri());
    }
}
