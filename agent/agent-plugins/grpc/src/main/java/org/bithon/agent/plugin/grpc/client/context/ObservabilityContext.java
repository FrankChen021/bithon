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

package org.bithon.agent.plugin.grpc.client.context;

import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.plugin.grpc.metrics.GrpcMetrics;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/25 8:49
 */
public class ObservabilityContext {
    private final String service;
    private final String method;
    private final String server;
    private final GrpcMetrics metrics;
    private final ITraceSpan span;
    private final long startTimeNs;

    public ObservabilityContext(String server, String grpcMethodFullName) {
        int separator = grpcMethodFullName.lastIndexOf('/');
        if (separator > 0) {
            this.service = grpcMethodFullName.substring(0, separator);
            this.method = grpcMethodFullName.substring(separator + 1);
        } else {
            this.service = "";
            this.method = grpcMethodFullName;
        }

        this.server = server;
        this.metrics = GrpcClientMetricStorage.getInstance().newMetrics();

        // Not sure when the other methods will be called,
        // so, it's better to create a new tracing context for this gRPC client call
        ITraceSpan clientSpan = TraceContextFactory.newAsyncSpan("grpc-client");
        if (clientSpan != null && clientSpan.context().traceMode().equals(TraceMode.TRACING)) {
            this.span = clientSpan;
        } else {
            this.span = null;
        }

        this.startTimeNs = System.nanoTime();
        start();
    }

    public void setRequestSize(long requestSize) {
        metrics.bytesSent = requestSize;
        if (span != null) {
            span.tag(Tags.Rpc.RPC_CLIENT_REQ_SIZE, requestSize);
        }
    }

    public void setResponseSize(long responseSize) {
        metrics.bytesReceived = responseSize;
        if (span != null) {
            span.tag(Tags.Rpc.RPC_CLIENT_RSP_SIZE, responseSize);
        }
    }

    public ITraceSpan getSpan() {
        return span;
    }

    public void start() {
        if (span != null) {
            span.method(service, method)
                .tag(Tags.Net.PEER, this.server)
                .kind(SpanKind.CLIENT)
                .start();
        }
    }

    public void finish(Throwable throwable) {
        if (span != null) {
            span.tag(throwable).finish();
            span.context().finish();
        }
        this.metrics.callCount = 1;
        this.metrics.errorCount = throwable == null ? 0 : 1;
        this.metrics.responseTime = System.nanoTime() - startTimeNs;
        this.metrics.maxResponseTime = this.metrics.responseTime;
        this.metrics.minResponseTime = this.metrics.responseTime;
    }

    public void finish(String status, Throwable throwable) {
        if (span != null) {
            span.tag("status", status)
                .tag(throwable)
                .finish();
            span.context().finish();
        }
        this.metrics.callCount = 1;
        this.metrics.errorCount = "OK".equals(status) ? 0 : 1;
        this.metrics.responseTime = System.nanoTime() - startTimeNs;
        this.metrics.maxResponseTime = this.metrics.responseTime;
        this.metrics.minResponseTime = this.metrics.responseTime;
    }
}
