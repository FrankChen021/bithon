package com.sbss.bithon.agent.core.metric.web;

import com.sbss.bithon.agent.core.metric.Timer;
import com.sbss.bithon.agent.core.metric.Sum;

/**
 * Web Request Counter
 *
 * @author frankchen
 */
public class WebRequestMetric {
    /**
     * dimension, the URI of a requests
     */
    private final String srcApplication;
    private final String uri;

    /**
     * metrics
     */
    private final Timer responseTime = new Timer();
    private final Sum requestCount = new Sum();
    private final Sum errorCount = new Sum();
    private final Sum count4xx = new Sum();
    private final Sum count5xx = new Sum();
    private final Sum requestByteSize = new Sum();
    private final Sum responseByteSize = new Sum();

    public WebRequestMetric(String srcApplication, String uri) {
        this.srcApplication = srcApplication;
        this.uri = uri;
    }

    public void add(long responseTime, int errorCount) {
        this.responseTime.update(responseTime);
        this.errorCount.update(errorCount);
        this.requestCount.incr();
    }

    public void add(long responseTime, int errorCount, int count4xx, int count5xx) {
        this.add(responseTime, errorCount);
        this.count4xx.update(count4xx);
        this.count5xx.update(count5xx);
    }

    public void addBytes(long requestByteSize, long responseByteSize) {
        if (requestByteSize > 0) {
            this.requestByteSize.update(requestByteSize);
        }
        if (responseByteSize > 0) {
            this.responseByteSize.update(responseByteSize);
        }
    }

    public String getSrcApplication() {
        return srcApplication;
    }

    public String getUri() {
        return uri;
    }

    public Timer getResponseTime() {
        return responseTime;
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public long getCount4xx() {
        return count4xx.get();
    }

    public long getCount5xx() {
        return count5xx.get();
    }

    public long getRequestByteSize() {
        return requestByteSize.get();
    }

    public long getResponseByteSize() {
        return responseByteSize.get();
    }
}
