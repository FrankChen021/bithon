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

package org.bithon.server.collector.source.brpc;

import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.event.BrpcEventMessage;
import org.bithon.agent.rpc.brpc.event.IEventCollector;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.event.sink.EventMessage;
import org.bithon.server.event.sink.IEventMessageSink;

import java.util.Collections;
import java.util.Iterator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 3:59 下午
 */
@Slf4j
public class BrpcEventCollector implements IEventCollector {

    private final IEventMessageSink eventSink;

    public BrpcEventCollector(IEventMessageSink eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public void sendEvent(BrpcMessageHeader header, BrpcEventMessage message) {
        EventMessage eventMessage = EventMessage.builder()
                                                .appName(header.getAppName())
                                                .instanceName(header.getInstanceName())
                                                .timestamp(message.getTimestamp())
                                                .type(message.getEventType())
                                                .args(message.getArgumentsMap())
                                                .build();
        Iterator<EventMessage> delegate = Collections.singletonList(eventMessage).iterator();
        eventSink.process("event", new CloseableIterator<EventMessage>() {
            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public EventMessage next() {
                return delegate.next();
            }
        });
    }
}
