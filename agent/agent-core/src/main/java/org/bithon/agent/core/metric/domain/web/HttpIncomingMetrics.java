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

package org.bithon.agent.core.metric.domain.web;

import org.bithon.agent.core.metric.model.ICompositeMetric;
import org.bithon.agent.sdk.metric.IMetricValueProvider;
import org.bithon.agent.sdk.metric.aggregator.LongMax;
import org.bithon.agent.sdk.metric.aggregator.LongMin;
import org.bithon.agent.sdk.metric.aggregator.LongSum;

/**
 * Web Request Counter
 *
 * @author frankchen
 */
public class HttpIncomingMetrics implements ICompositeMetric {
    private final LongSum responseTime = new LongSum();
    private final LongMax maxResponseTime = new LongMax();
    private final LongMin minResponseTime = new LongMin();
    private final LongSum totalCount = new LongSum();
    private final LongSum okCount = new LongSum();
    private final LongSum errorCount = new LongSum();
    private final LongSum count4xx = new LongSum();
    private final LongSum count5xx = new LongSum();
    private final LongSum requestBytes = new LongSum();
    private final LongSum responseBytes = new LongSum();
    private final LongSum flowedCount = new LongSum();
    private final LongSum degradedCount = new LongSum();

    private void updateRequest(long responseTime, boolean isError) {
        this.responseTime.update(responseTime);
        this.maxResponseTime.update(responseTime);
        this.minResponseTime.update(responseTime);
        if (isError) {
            this.errorCount.incr();
        } else {
            this.okCount.incr();
        }
        this.totalCount.incr();
    }

    public HttpIncomingMetrics updateRequest(long responseTime, int count4xx, int count5xx) {
        this.updateRequest(responseTime, count4xx > 0 || count5xx > 0);
        this.count4xx.update(count4xx);
        this.count5xx.update(count5xx);
        return this;
    }

    public HttpIncomingMetrics updateBytes(long requestByteSize, long responseByteSize) {
        if (requestByteSize > 0) {
            this.requestBytes.update(requestByteSize);
        }
        if (responseByteSize > 0) {
            this.responseBytes.update(responseByteSize);
        }
        return this;
    }

    public LongSum getResponseTime() {
        return responseTime;
    }

    public LongMax getMaxResponseTime() {
        return maxResponseTime;
    }

    private LongMin getMinResponseTime() {
        return minResponseTime;
    }

    public LongSum getTotalCount() {
        return totalCount;
    }

    public LongSum getErrorCount() {
        return errorCount;
    }

    public LongSum getOkCount() {
        return okCount;
    }

    public LongSum getCount4xx() {
        return count4xx;
    }

    public LongSum getCount5xx() {
        return count5xx;
    }

    public LongSum getRequestBytes() {
        return requestBytes;
    }

    public LongSum getResponseBytes() {
        return responseBytes;
    }

    public LongSum getFlowedCount() {
        return flowedCount;
    }

    public LongSum getDegradedCount() {
        return degradedCount;
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return new IMetricValueProvider[]{
            responseTime,
            maxResponseTime,
            minResponseTime,
            totalCount,
            okCount,
            errorCount,
            count4xx,
            count5xx,
            requestBytes,
            responseBytes,
            flowedCount,
            degradedCount,
            };
    }
}
