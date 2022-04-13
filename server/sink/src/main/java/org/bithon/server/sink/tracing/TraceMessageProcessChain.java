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

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 12/4/22 11:38 AM
 */
@Slf4j
public class TraceMessageProcessChain implements ITraceMessageSink {
    private final List<ITraceMessageSink> sinks = Collections.synchronizedList(new ArrayList<>());

    public TraceMessageProcessChain(ITraceMessageSink sink) {
        link(sink);
    }

    public ITraceMessageSink link(ITraceMessageSink sink) {
        sinks.add(sink);
        return sink;
    }

    public ITraceMessageSink unlink(ITraceMessageSink sink) {
        sinks.remove(sink);
        return sink;
    }

    @Override
    public void process(String messageType, List<TraceSpan> spans) {
        for (ITraceMessageSink sink : sinks) {
            try {
                sink.process(messageType, spans);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (ITraceMessageSink sink : sinks) {
            sink.close();
        }
    }
}
