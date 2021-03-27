package com.sbss.bithon.agent.core.metric.domain.mongo;

import com.sbss.bithon.agent.core.metric.model.ICompositeMetric;
import com.sbss.bithon.agent.core.metric.model.Sum;
import com.sbss.bithon.agent.core.metric.model.Timer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:35 下午
 */
public class MongoClientCompositeMetric implements ICompositeMetric {
    Timer responseTime = new Timer();
    Sum callCount = new Sum();
    Sum exceptionCount = new Sum();
    Sum bytesIn = new Sum();
    Sum bytesOut = new Sum();

    public void add(long responseTime, int exceptionCount) {
        this.callCount.incr();
        this.responseTime.update(responseTime);
        this.exceptionCount.update(exceptionCount);
    }

    public void addBytesIn(int bytesIn) {
        this.bytesIn.update(bytesIn);
    }

    public void addBytesOut(int bytesOut) {
        this.bytesOut.update(bytesOut);
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

    public Sum getBytesIn() {
        return bytesIn;
    }

    public Sum getBytesOut() {
        return bytesOut;
    }
}
