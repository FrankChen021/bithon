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

package org.bithon.server.sink.tracing.source;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.springframework.context.ApplicationContext;

/**
 * @author Frank Chen
 * @date 22/1/24 11:09 pm
 */
public class BrpcSource implements ITraceMessageSource {

    @JsonCreator
    public BrpcSource(@JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext context) {
    }

    @Override
    public void start() {
    }

    @Override
    public void registerProcessor(ITraceMessageSink processor) {
    }

    @Override
    public void stop() {

    }
}
