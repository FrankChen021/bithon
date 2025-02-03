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

package org.bithon.agent.observability.metric.domain.sql;

import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.Max;
import org.bithon.agent.observability.metric.model.Min;
import org.bithon.agent.observability.metric.model.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:18 下午
 */
public class SQLMetrics implements IMetricSet {
    private final Min minResponseTime = new Min();
    private final Sum responseTime = new Sum();
    private final Max maxResponseTime = new Max();
    private final Sum callCount = new Sum();
    private final Sum errorCount = new Sum();
    private final Sum queryCount = new Sum();
    private final Sum updateCount = new Sum();
    private final Sum bytesIn = new Sum();
    private final Sum bytesOut = new Sum();

    private final IMetricValueProvider[] metrics = new IMetricValueProvider[]{
        minResponseTime,
        responseTime,
        maxResponseTime,
        callCount,
        errorCount,
        queryCount,
        updateCount,
        bytesIn,
        bytesOut
    };

    public SQLMetrics update(Boolean isQuery, boolean failed, long responseTime) {
        this.responseTime.update(responseTime);
        this.minResponseTime.update(responseTime);
        this.maxResponseTime.update(responseTime);
        if (isQuery != null) {
            if (isQuery) {
                this.queryCount.incr();
            } else {
                this.updateCount.incr();
            }
        }

        if (failed) {
            this.errorCount.incr();
        }

        this.callCount.incr();

        return this;
    }

    public long peekTotalCount() {
        return callCount.peek();
    }

    public void updateBytesIn(int bytesIn) {
        this.bytesIn.update(bytesIn);
    }

    public void updateBytesOut(int bytesOut) {
        this.bytesOut.update(bytesOut);
    }

    public Sum getResponseTime() {
        return responseTime;
    }

    public Sum getCallCount() {
        return callCount;
    }

    public Sum getErrorCount() {
        return errorCount;
    }

    public Sum getQueryCount() {
        return queryCount;
    }

    public Sum getUpdateCount() {
        return updateCount;
    }

    public Sum getBytesIn() {
        return bytesIn;
    }

    public Sum getBytesOut() {
        return bytesOut;
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return metrics;
    }
}
