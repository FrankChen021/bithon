/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.collector.kafka;

import com.sbss.bithon.server.collector.sink.local.LocalTraceSink;
import com.sbss.bithon.server.tracing.handler.TraceMessageHandler;
import com.sbss.bithon.server.tracing.handler.TraceSpan;

import java.util.ArrayList;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaTraceCollector extends AbstractKafkaCollector<TraceMessage> {
    private final LocalTraceSink localSink;

    public KafkaTraceCollector(TraceMessageHandler traceMessageHandler) {
        super(TraceMessage.class);

        localSink = new LocalTraceSink(traceMessageHandler);
    }

    @Override
    protected String getGroupId() {
        return "bithon-collector-trace";
    }

    @Override
    protected String[] getTopics() {
        return new String[]{"trace"};
    }

    @Override
    protected void onMessage(String topic, TraceMessage traceSpans) {
        localSink.process(topic, traceSpans);
    }
}

class TraceMessage extends ArrayList<TraceSpan> {
}
