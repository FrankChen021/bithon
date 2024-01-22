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

package org.bithon.server.sink.tracing.sink;

import lombok.Getter;
import org.bithon.server.sink.tracing.TraceSinkHandler;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * @author Frank Chen
 * @date 22/1/24 10:41 pm
 */
public class SinkToStorage implements ITraceMessageSink2 {

    @Getter
    private final TraceSinkHandler handler;

    public SinkToStorage(ApplicationContext applicationContext) {
        this.handler = new TraceSinkHandler(applicationContext);
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
