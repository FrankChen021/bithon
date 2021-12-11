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

package org.bithon.agent.core.metric.domain.mongo;

import org.bithon.agent.core.metric.model.ICompositeMetric;
import org.bithon.agent.core.metric.model.Sum;
import org.bithon.agent.core.metric.model.Timer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:35 下午
 */
public class MongoDbMetrics implements ICompositeMetric {
    Timer responseTime = new Timer();
    Sum callCount = new Sum();
    Sum exceptionCount = new Sum();
    Sum responseBytes = new Sum();
    Sum requestBytes = new Sum();

    public void add(long responseTime, int exceptionCount) {
        this.callCount.incr();
        this.responseTime.update(responseTime);
        this.exceptionCount.update(exceptionCount);
    }

    public void addBytesIn(int bytesIn) {
        this.responseBytes.update(bytesIn);
    }

    public void addBytesOut(int bytesOut) {
        this.requestBytes.update(bytesOut);
    }

    public Timer getResponseTime() {
        return responseTime;
    }

    public Sum getCallCount() {
        return callCount;
    }

    public Sum getExceptionCount() {
        return exceptionCount;
    }

    public Sum getResponseBytes() {
        return responseBytes;
    }

    public Sum getRequestBytes() {
        return requestBytes;
    }
}
