package com.sbss.bithon.agent.plugin.tomcat.metric;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;
import com.sbss.bithon.agent.core.metric.domain.web.WebRequestCompositeMetric;
import org.apache.coyote.Request;
import org.apache.coyote.Response;

import java.util.List;

/**
 * @author frankchen
 */
public class WebRequestMetricCollector extends IntervalMetricCollector<WebRequestCompositeMetric> {

    public void update(Request request, Response response, long responseTime) {
        String uri = request.requestURI().toString();
        if (uri == null) {
            return;
        }

        String srcApplication = request.getHeader(InterceptorContext.HEADER_SRC_APPLICATION_NAME);

        int httpStatus = response.getStatus();
        int errorCount = response.getStatus() >= 400 ? 1 : 0;
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 && httpStatus < 600 ? 1 : 0;
        long requestByteSize = request.getBytesRead();
        long responseByteSize = response.getBytesWritten(false);

        WebRequestCompositeMetric metric = getOrCreateMetric(srcApplication == null ? "" : srcApplication, uri);
        metric.updateRequest(responseTime, errorCount, count4xx, count5xx);
        metric.updateBytes(requestByteSize, responseByteSize);
    }

    @Override
    protected WebRequestCompositeMetric newMetrics() {
        return new WebRequestCompositeMetric();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               WebRequestCompositeMetric metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }
}
