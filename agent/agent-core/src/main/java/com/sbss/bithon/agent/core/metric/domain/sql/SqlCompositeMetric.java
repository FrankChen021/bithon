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

package com.sbss.bithon.agent.core.metric.domain.sql;

import com.sbss.bithon.agent.core.metric.model.ICompositeMetric;
import com.sbss.bithon.agent.core.metric.model.Sum;
import com.sbss.bithon.agent.core.metric.model.Timer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:18 下午
 */
public class SqlCompositeMetric implements ICompositeMetric {
    private final Timer responseTime = new Timer();
    private final Sum callCount = new Sum();
    private final Sum errorCount = new Sum();
    private final Sum queryCount = new Sum();
    private final Sum updateCount = new Sum();
    private final Sum bytesIn = new Sum();
    private final Sum bytesOut = new Sum();

    public void update(boolean isQuery, boolean failed, long responseTime) {
        this.responseTime.update(responseTime);
        if (isQuery) {
            this.queryCount.incr();
        } else {
            this.updateCount.incr();
        }

        if (failed) {
            this.errorCount.incr();
        }

        this.callCount.incr();
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

    public Timer getResponseTime() {
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
}
