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

package org.bithon.agent.plugin.redis.redisson.interceptor;

import io.netty.buffer.ByteBuf;
import org.bithon.agent.observability.metric.domain.redis.RedisMetricRegistry;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/5/4 22:31
 */
public class CommandContext {
    public String endpoint;
    public String command;
    public int dbIndex;
    public long startNanoTime;
    public long requestBytes;
    public ByteBuf responseBuf;
    public long responseBufStartIndex;
    public ITraceSpan span;

    public CommandContext(String endpoint, int dbIndex) {
        this.endpoint = endpoint;
        this.dbIndex = dbIndex;
    }

    public void begin(String command) {
        this.command = command;
        this.startNanoTime = System.nanoTime();
        this.span = TraceContextFactory.newAsyncSpan("redisson");
        if (span != null) {
            span.method("org.redisson.client.RedisConnection", "send")
                .kind(SpanKind.CLIENT)
                .tag(Tags.Database.SYSTEM, "redis")
                .tag(Tags.Database.REDIS_DB_INDEX, this.dbIndex)
                .tag(Tags.Database.OPERATION, command)
                .tag(Tags.Net.PEER, this.endpoint)
                .start();
        }
    }

    public void complete(boolean isException) {
        RedisMetricRegistry.get()
                           .getOrCreateMetrics(this.endpoint, this.command)
                           .addRequest(System.nanoTime() - startNanoTime, isException ? 1 : 0)
                           .addRequestBytes(this.requestBytes)
                           .addResponseBytes(this.responseBuf == null ? 0 : this.responseBuf.readerIndex() - this.responseBufStartIndex);
        this.responseBuf = null;

        if (this.span != null) {
            this.span.finish();
            this.span.context().finish();
            this.span = null;
        }
    }
}
