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

package org.bithon.server.collector.otlp.http;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.pipeline.tracing.receiver.ITraceReceiver;
import org.springframework.boot.autoconfigure.web.ServerProperties;

/**
 * @author Frank Chen
 * @date 30/1/24 9:08 pm
 */
@Slf4j
@JsonTypeName("otlp-trace-http")
public class OtlpHttpTraceReceiverEnabler implements ITraceReceiver {

    private final OtlpHttpTraceReceiver receiver;
    private final int serverPort;

    @JsonCreator
    public OtlpHttpTraceReceiverEnabler(@JacksonInject(useInput = OptBoolean.FALSE) OtlpHttpTraceReceiver receiver,
                                        @JacksonInject(useInput = OptBoolean.FALSE) ServerProperties serverProperties) {
        this.receiver = receiver;
        this.serverPort = serverProperties.getPort();
    }

    @Override
    public void start() {
        log.info("Starting otlp-trace over HTTP at port {}", this.serverPort);
    }

    @Override
    public void stop() {
        log.info("Stopping otlp-trace over HTTP at port {}", this.serverPort);

        this.receiver.setProcessor(null);
    }

    @Override
    public void registerProcessor(ITraceProcessor processor) {
        this.receiver.setProcessor(processor);
    }
}
