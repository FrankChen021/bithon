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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.event.BrpcEventMessage;
import org.bithon.agent.rpc.brpc.event.IEventCollector;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.sink.event.IEventProcessor;
import org.bithon.server.sink.event.receiver.IEventReceiver;
import org.bithon.server.storage.event.EventMessage;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 3:59 下午
 */
@Slf4j
@JsonTypeName("brpc")
public class BrpcEventCollector implements IEventCollector, IEventReceiver {

    private final BrpcCollectorServer server;
    private final int port;

    private IEventProcessor processor;
    private BrpcCollectorServer.ServiceGroup serviceGroup;

    @JsonCreator
    public BrpcEventCollector(@JacksonInject(useInput = OptBoolean.FALSE) BrpcCollectorConfig config,
                              @JacksonInject(useInput = OptBoolean.FALSE) BrpcCollectorServer server) {
        Preconditions.checkNotNull(config.getPort(), "The brpc server port is not configured.");

        Integer port = config.getPort().get("event");
        Preconditions.checkNotNull(port, "The port for the event collector is not configured.");

        this.server = server;
        this.port = port;
    }

    @Override
    public void start() {
        serviceGroup = this.server.addService("event", this, port);
    }

    @Override
    public void registerProcessor(IEventProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void stop() {
        serviceGroup.stop("metrics");
    }

    @Override
    public void sendEvent(BrpcMessageHeader header, BrpcEventMessage message) {
        EventMessage eventMessage = EventMessage.builder()
                                                .appName(header.getAppName())
                                                .instanceName(header.getInstanceName())
                                                .timestamp(message.getTimestamp())
                                                .type(message.getEventType())
                                                .jsonArgs(message.getJsonArguments())
                                                .build();
        processor.process("event", Collections.singletonList(eventMessage));
    }

    @Override
    public void sendEvent2(BrpcMessageHeader header, List<BrpcEventMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        processor.process("event",
                          messages.stream()
                                  .map((message) -> EventMessage.builder()
                                                                .appName(header.getAppName())
                                                                .instanceName(header.getInstanceName())
                                                                .timestamp(message.getTimestamp())
                                                                .type(message.getEventType())
                                                                .jsonArgs(message.getJsonArguments())
                                                                .build()).collect(Collectors.toList()));
    }
}
