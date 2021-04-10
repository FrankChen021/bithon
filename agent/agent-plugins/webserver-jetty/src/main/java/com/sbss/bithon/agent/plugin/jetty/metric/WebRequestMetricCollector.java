package com.sbss.bithon.agent.plugin.jetty.metric;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;
import com.sbss.bithon.agent.core.metric.domain.web.WebRequestCompositeMetric;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


/**
 * @author frankchen
 */
public class WebRequestMetricCollector extends IntervalMetricCollector<WebRequestCompositeMetric> {

    public void update(
        Request request,
        HttpServletRequest httpServletRequest,
        HttpServletResponse response,
        long costTime
    ) {
        String srcApplication = request.getHeader(InterceptorContext.HEADER_SRC_APPLICATION_NAME);
        String uri = httpServletRequest.getRequestURI();
        int httpStatus = response.getStatus();
        int errorCount = response.getStatus() >= 400 ? 1 : 0;
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 && httpStatus < 600 ? 1 : 0;
        long requestByteSize = request.getContentRead();
        long responseByteSize = 0;
        if (response instanceof Response) {
            Response jettyResponse = (Response) response;
            responseByteSize = jettyResponse.getContentCount();
        }

        WebRequestCompositeMetric webRequestMetricsSet = getOrCreateMetric(srcApplication == null ? "" : srcApplication,
                                                                           uri);
        webRequestMetricsSet.updateRequest(costTime, errorCount, count4xx, count5xx);
        webRequestMetricsSet.updateBytes(requestByteSize, responseByteSize);
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
