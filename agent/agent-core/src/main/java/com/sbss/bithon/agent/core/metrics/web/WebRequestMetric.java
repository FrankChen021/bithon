package com.sbss.bithon.agent.core.metrics.web;

import com.sbss.bithon.agent.core.metrics.Counter;

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
    private final Counter costTime = new Counter();
    private final Counter requestCount = new Counter();
    private final Counter errorCount = new Counter();
    private final Counter count4xx = new Counter();
    private final Counter count5xx = new Counter();
    private final Counter requestByteSize = new Counter();
    private final Counter responseByteSize = new Counter();

    public WebRequestMetric(String srcApplication, String uri) {
        this.srcApplication = srcApplication;
        this.uri = uri;
    }

    public void add(long costTime, int errorCount) {
        this.costTime.add(costTime);
        this.errorCount.add(errorCount);
        this.requestCount.incr();
    }

    public void add(long costTime, int errorCount, int count4xx, int count5xx) {
        this.add(costTime, errorCount);
        this.count4xx.add(count4xx);
        this.count5xx.add(count5xx);
    }

    public void addBytes(long requestByteSize, long responseByteSize) {
        if (requestByteSize > 0) {
            this.requestByteSize.add(requestByteSize);
        }
        if (responseByteSize > 0) {
            this.responseByteSize.add(responseByteSize);
        }
    }

    public String getSrcApplication() {
        return srcApplication;
    }

    public String getUri() {
        return uri;
    }

    public long getCostTime() {
        return costTime.get();
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
