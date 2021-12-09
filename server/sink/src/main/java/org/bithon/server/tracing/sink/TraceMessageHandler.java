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

package org.bithon.server.tracing.sink;

import com.google.common.collect.ImmutableList;
import org.bithon.server.common.handler.AbstractThreadPoolMessageHandler;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.bithon.server.tracing.storage.ITraceWriter;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:21 下午
 */
public class TraceMessageHandler extends AbstractThreadPoolMessageHandler<CloseableIterator<TraceSpan>> {

    final ITraceWriter traceWriter;

    public TraceMessageHandler(ApplicationContext applicationContext) {
        super("trace",
              2,
              10,
              Duration.ofMinutes(1),
              2048);

        this.traceWriter = applicationContext.getBean(ITraceStorage.class).createWriter();
    }

    @Override
    protected void onMessage(CloseableIterator<TraceSpan> traceSpans) throws IOException {
        traceWriter.write(ImmutableList.copyOf(traceSpans));
    }

    @Override
    public String getType() {
        return "trace";
    }
}
