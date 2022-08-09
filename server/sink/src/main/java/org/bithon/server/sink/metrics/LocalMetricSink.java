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

package org.bithon.server.sink.metrics;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.List;

/**
 * This sink is designed for function evaluation and local development.
 * It calls message handlers directly in process.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@Slf4j
@JsonTypeName("local")
public class LocalMetricSink implements IMetricMessageSink {

    private MetricMessageHandlers handlers;

    @JsonCreator
    public LocalMetricSink(@JacksonInject(useInput = OptBoolean.FALSE) MetricMessageHandlers handlers) {
        this.handlers = handlers;
    }

    @Override
    public void process(String messageType, List<IInputRow> messages) {
        MetricMessageHandler handler = handlers.getHandler(messageType);
        if (handler != null) {
            handler.process(messages);
        } else {
            log.error("No Handler for message [{}]", messageType);
        }
    }

    @Override
    public void close() throws Exception {
        for (MetricMessageHandler handler : handlers.getHandlers()) {
            handler.close();
        }
    }
}
