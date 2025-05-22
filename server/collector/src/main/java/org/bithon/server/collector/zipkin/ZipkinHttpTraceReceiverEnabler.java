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

package org.bithon.server.collector.zipkin;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.pipeline.tracing.receiver.ITraceReceiver;
import org.springframework.boot.autoconfigure.web.ServerProperties;

/**
 * @author frank.chen021@outlook.com
 */
@Slf4j
@JsonTypeName("zipkin-trace-http")
public class ZipkinHttpTraceReceiverEnabler implements ITraceReceiver {

    private final ZipkinHttpTraceReceiver receiver;
    private final int serverPort;

    @JsonCreator
    public ZipkinHttpTraceReceiverEnabler(@JacksonInject(useInput = OptBoolean.FALSE) ZipkinHttpTraceReceiver receiver,
                                          @JacksonInject(useInput = OptBoolean.FALSE) ServerProperties serverProperties) {
        this.receiver = receiver;
        this.serverPort = serverProperties.getPort();
    }

    @Override
    public void registerProcessor(ITraceProcessor processor) {
        receiver.setProcessor(processor);
    }

    @Override
    public void start() {
        log.info("Starting zipkin-trace-receiver receiver over HTTP at port {}", this.serverPort);
    }

    @Override
    public void stop() {
        log.info("Stopping zipkin-trace-receiver over HTTP at port {}", this.serverPort);

        this.receiver.setProcessor(null);
    }
}
