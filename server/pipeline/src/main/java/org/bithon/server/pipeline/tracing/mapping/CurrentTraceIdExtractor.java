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

import org.bithon.server.storage.tracing.TraceSpan;

import java.util.function.BiConsumer;

/**
 * Extract trace id itself as a mapping-entry.
 * This helps to improve the search performance when searching trace by given trace-id
 */
class CurrentTraceIdExtractor implements ITraceIdMappingExtractor {
    static final ITraceIdMappingExtractor INSTANCE = new CurrentTraceIdExtractor();

    @Override
    public void extract(TraceSpan span, BiConsumer<TraceSpan, String> consumer) {
        consumer.accept(span, span.getTraceId());
    }
}
