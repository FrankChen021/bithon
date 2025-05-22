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

package org.bithon.server.collector.http;

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
 * @date 30/1/24 9:09 pm
 */
@Slf4j
@JsonTypeName("bithon-trace-http")
public class BithonHttpTraceEnabler implements ITraceReceiver {

    private final TraceHttpCollector collector;
    private final int serverPort;

    @JsonCreator
    public BithonHttpTraceEnabler(@JacksonInject(useInput = OptBoolean.FALSE) TraceHttpCollector collector,
                                  @JacksonInject(useInput = OptBoolean.FALSE) ServerProperties serverProperties) {
        this.collector = collector;
        this.serverPort = serverProperties.getPort();
    }

    @Override
    public void start() {
        log.info("Starting bithon-trace-receiver over HTTP at {}", this.serverPort);
    }

    @Override
    public void stop() {
        log.info("Stopping bithon-trace-receiver over HTTP at {}", this.serverPort);
        this.collector.setProcessor(null);
    }

    @Override
    public void registerProcessor(ITraceProcessor processor) {
        this.collector.setProcessor(processor);
    }
}
