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

package org.bithon.server.sink.tracing;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
@JsonTypeName("local")
public class LocalTraceSink implements ITraceMessageSink {

    @Getter
    private final TraceMessageHandler handler;

    @JsonCreator
    public LocalTraceSink(@JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.handler = new TraceMessageHandler(applicationContext);
    }

    @Override
    public void process(String messageType, List<TraceSpan> messages) {
        handler.submit(messages);
    }

    @Override
    public void close() throws Exception {
        handler.close();
    }
}
