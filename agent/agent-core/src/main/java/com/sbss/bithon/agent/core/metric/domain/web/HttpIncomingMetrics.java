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

package com.sbss.bithon.agent.core.metric.domain.web;

import com.sbss.bithon.agent.core.metric.model.ICompositeMetric;
import com.sbss.bithon.agent.core.metric.model.Sum;
import com.sbss.bithon.agent.core.metric.model.Timer;

/**
 * Web Request Counter
 *
 * @author frankchen
 */
public class HttpIncomingMetrics implements ICompositeMetric {
    private final Timer responseTime = new Timer();
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
        if (isError) {
            this.errorCount.incr();
        } else {
            this.okCount.incr();
        }
        this.totalCount.incr();
    }

    public void updateRequest(long responseTime, int count4xx, int count5xx) {
        this.updateRequest(responseTime, count4xx > 0 || count5xx > 0);
        this.count4xx.update(count4xx);
        this.count5xx.update(count5xx);
    }

    public void updateBytes(long requestByteSize, long responseByteSize) {
        if (requestByteSize > 0) {
            this.requestBytes.update(requestByteSize);
        }
        if (responseByteSize > 0) {
            this.responseBytes.update(responseByteSize);
        }
    }

    public Timer getResponseTime() {
        return responseTime;
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
}
