package com.sbss.bithon.agent.core.metric.domain.web;

import com.sbss.bithon.agent.core.metric.model.ICompositeMetric;
import com.sbss.bithon.agent.core.metric.model.Timer;
import com.sbss.bithon.agent.core.metric.model.Sum;

/**
 * Web Request Counter
 *
 * @author frankchen
 */
public class WebRequestCompositeMetric implements ICompositeMetric {
    private final Timer responseTime = new Timer();
    private final Sum requestCount = new Sum();
    private final Sum errorCount = new Sum();
    private final Sum count4xx = new Sum();
    private final Sum count5xx = new Sum();
    private final Sum requestBytes = new Sum();
    private final Sum responseBytes = new Sum();

    public void updateRequest(long responseTime, int errorCount) {
        this.responseTime.update(responseTime);
        this.errorCount.update(errorCount);
        this.requestCount.incr();
    }

    public void updateRequest(long responseTime, int errorCount, int count4xx, int count5xx) {
        this.updateRequest(responseTime, errorCount);
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

    public Sum getRequestCount() {
        return requestCount;
    }

    public Sum getErrorCount() {
        return errorCount;
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
}
