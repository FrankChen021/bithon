/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.collector.sink.local;

import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.tracing.handler.TraceMessageHandler;
import com.sbss.bithon.server.tracing.handler.TraceSpan;
import lombok.Getter;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class LocalTraceSink implements IMessageSink<List<TraceSpan>> {

    @Getter
    private final TraceMessageHandler traceMessageHandler;

    public LocalTraceSink(TraceMessageHandler traceMessageHandler) {
        this.traceMessageHandler = traceMessageHandler;
    }

    @Override
    public void process(String messageType, List<TraceSpan> message) {
        traceMessageHandler.submit(message);
    }
}
