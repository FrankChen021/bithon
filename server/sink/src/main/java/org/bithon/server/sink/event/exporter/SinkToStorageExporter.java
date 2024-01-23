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

package org.bithon.server.sink.event.exporter;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.sink.event.EventPipelineConfig;
import org.bithon.server.storage.event.EventMessage;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.event.IEventWriter;

import java.io.IOException;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/29 21:34
 */
@Slf4j
public class SinkToStorageExporter implements IEventExporter {
    private final IEventWriter writer;

    @JsonCreator
    public SinkToStorageExporter(@JacksonInject(useInput = OptBoolean.FALSE) IEventStorage eventStorage,
                                 @JacksonInject(useInput = OptBoolean.FALSE) EventPipelineConfig pipelineConfig) {
        this.writer = new EventBatchWriter(eventStorage.createWriter(), pipelineConfig);
    }

    @Override
    public void process(String messageType, List<EventMessage> messages) {
        try {
            writer.write(messages);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        this.writer.close();
    }
}
