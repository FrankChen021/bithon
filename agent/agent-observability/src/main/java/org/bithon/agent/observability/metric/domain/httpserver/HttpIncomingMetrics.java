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

package org.bithon.agent.observability.metric.domain.httpserver;

import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.Max;
import org.bithon.agent.observability.metric.model.Min;
import org.bithon.agent.observability.metric.model.Sum;


/**
 * Web Request Counter
 *
 * @author frankchen
 */
public class HttpIncomingMetrics implements IMetricSet {
    private final Sum responseTime = new Sum();
    private final Max maxResponseTime = new Max();
    private final Min minResponseTime = new Min();
    private final Sum totalCount = new Sum();
    private final Sum okCount = new Sum();
    private final Sum errorCount = new Sum();
    private final Sum count4xx = new Sum();
    private final Sum count5xx = new Sum();
    private final Sum requestBytes = new Sum();
    private final Sum responseBytes = new Sum();
    private final Sum flowedCount = new Sum();
    private final Sum degradedCount = new Sum();

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

    public Sum getResponseTime() {
        return responseTime;
    }

    public Max getMaxResponseTime() {
        return maxResponseTime;
    }

    private Min getMinResponseTime() {
        return minResponseTime;
    }

    public Sum getTotalCount() {
        return totalCount;
    }

    public Sum getErrorCount() {
        return errorCount;
    }

    public Sum getOkCount() {
        return okCount;
    }

    public Sum getCount4xx() {
        return count4xx;
    }

    public Sum getCount5xx() {
        return count5xx;
    }

    public Sum getRequestBytes() {
        return requestBytes;
    }

    public Sum getResponseBytes() {
        return responseBytes;
    }

    public Sum getFlowedCount() {
        return flowedCount;
    }

    public Sum getDegradedCount() {
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
            degradedCount
        };
    }
}
