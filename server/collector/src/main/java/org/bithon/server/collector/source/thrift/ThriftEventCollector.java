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

package org.bithon.server.collector.source.thrift;

import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.thrift.service.MessageHeader;
import org.bithon.agent.rpc.thrift.service.event.IEventCollector;
import org.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;
import org.bithon.server.collector.sink.IMessageSink;
import org.bithon.server.event.handler.EventMessage;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 3:59 下午
 */
@Slf4j
public class ThriftEventCollector implements IEventCollector.Iface {

    private final IMessageSink<EventMessage> eventSink;

    public ThriftEventCollector(IMessageSink<EventMessage> eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public void sendEvent(MessageHeader header, ThriftEventMessage message) {
        EventMessage eventMessage = EventMessage.builder().appName(header.getAppName())
                                                .instanceName(header.getInstanceName())
                                                .timestamp(message.getTimestamp())
                                                .type(message.getEventType())
                                                .args(message.getArguments())
                                                .build();
        eventSink.process("event", eventMessage);
    }
}
