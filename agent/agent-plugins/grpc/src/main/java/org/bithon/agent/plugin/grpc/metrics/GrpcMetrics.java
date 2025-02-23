package org.bithon.agent.plugin.grpc.metrics;

import org.bithon.agent.observability.metric.model.annotation.Max;
import org.bithon.agent.observability.metric.model.annotation.Min;
import org.bithon.agent.observability.metric.model.annotation.Sum;

public class GrpcMetrics {
    /**
     * in nanoseconds
     */
    @Sum
    public long responseTime;

    @Min
    public long minResponseTime;

    @Max
    public long maxResponseTime;

    @Sum
    public long callCount;

    @Sum
    public long errorCount;

    @Sum
    public long bytesSent;

    @Sum
    public long bytesReceived;
}
