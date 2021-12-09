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

package org.bithon.server.collector.source.kafka;

import org.bithon.server.tracing.sink.LocalTraceSink;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.tracing.sink.TraceSpan;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaTraceCollector extends AbstractKafkaCollector<TraceSpan> {
    private final LocalTraceSink localSink;

    public KafkaTraceCollector(LocalTraceSink localSink) {
        super(TraceSpan.class);

        this.localSink = localSink;
    }

    @Override
    protected String getGroupId() {
        return "bithon-trace-consumer";
    }

    @Override
    protected String getTopic() {
        return "bithon-trace";
    }

    @Override
    protected void onMessage(String type, CloseableIterator<TraceSpan> traceMessages) {
        localSink.process(getTopic(), traceMessages);
    }
}
