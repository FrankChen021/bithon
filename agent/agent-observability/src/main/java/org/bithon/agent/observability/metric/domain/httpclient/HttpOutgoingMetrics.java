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

package org.bithon.agent.observability.metric.domain.httpclient;

import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.Max;
import org.bithon.agent.observability.metric.model.Min;
import org.bithon.agent.observability.metric.model.Sum;

/**
 * NOTE: the order of ALL fields in this class must be consistent with the order in getMetrics method
 *
 * @author frankchen
 */
public class HttpOutgoingMetrics implements IMetricSet {
    /**
     * total cost time in NANO second
     */
    private final Sum responseTime = new Sum();
    private final Max maxResponseTime = new Max();
    private final Min minResponseTime = new Min();

    /**
     * count of all status code between 400(inclusive) and 500(exclusive)
     */
    private final Sum count4xx = new Sum();

    /**
     * count of all status code larger than 500(inclusive)
     */
    private final Sum count5xx = new Sum();
    private final Sum countException = new Sum();
    private final Sum requestCount = new Sum();
    private final Sum requestBytes = new Sum();
    private final Sum responseBytes = new Sum();

    public void add(long responseTime, int count4xx, int count5xx) {
        this.responseTime.update(responseTime);
        this.maxResponseTime.update(responseTime);
        this.minResponseTime.update(responseTime);
        this.count4xx.update(count4xx);
        this.count5xx.update(count5xx);
        this.requestCount.incr();
    }

    public HttpOutgoingMetrics addException(long responseTime, int exceptionCount) {
        this.responseTime.update(responseTime);
        this.maxResponseTime.update(responseTime);
        this.minResponseTime.update(responseTime);
        this.countException.update(exceptionCount);
        this.requestCount.incr();
        return this;
    }

    public void updateIOMetrics(long requestByteSize, long responseByteSize) {
        this.requestBytes.update(requestByteSize);
        this.responseBytes.update(responseByteSize);
    }

    public Sum getRequestBytes() {
        return requestBytes;
    }

    public Sum getResponseBytes() {
        return responseBytes;
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return new IMetricValueProvider[]{
            responseTime,
            maxResponseTime,
            minResponseTime,
            count4xx,
            count5xx,
            countException,
            requestCount,
            requestBytes,
            responseBytes
        };
    }
}
