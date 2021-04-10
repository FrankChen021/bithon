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

import com.sbss.bithon.server.collector.sink.local.LocalEventSink;
import com.sbss.bithon.server.event.handler.EventMessage;
import com.sbss.bithon.server.event.handler.EventsMessageHandler;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaEventCollector extends AbstractKafkaCollector<EventMessage> {
    private final LocalEventSink localSink;

    public KafkaEventCollector(EventsMessageHandler handler) {
        super(EventMessage.class);
        localSink = new LocalEventSink(handler);
    }

    @Override
    protected String getGroupId() {
        return "bithon-collector-event";
    }

    @Override
    protected String[] getTopics() {
        return new String[]{"event"};
    }

    @Override
    protected void onMessage(String topic, EventMessage eventMessage) {
        localSink.process(topic, eventMessage);
    }
}
