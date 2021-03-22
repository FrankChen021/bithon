package com.sbss.bithon.agent.plugin.jetty.metric;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;
import com.sbss.bithon.agent.core.metric.web.WebRequestMetricSet;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author frankchen
 */
public class WebRequestMetricCollector implements IMetricCollector {

    private final Map<String, WebRequestMetricSet> metricsMap = new ConcurrentHashMap<>();
    private WebRequestMetricSet metric;

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

        WebRequestMetricSet webRequestMetricsSet = metricsMap.computeIfAbsent(srcApplication + "|" + uri,
                                                                        key -> new WebRequestMetricSet(srcApplication,
                                                                                                       uri));
        webRequestMetricsSet.updateRequest(costTime, errorCount, count4xx, count5xx);
        webRequestMetricsSet.updateBytes(requestByteSize, responseByteSize);
    }

    @Override
    public boolean isEmpty() {
        return metricsMap.isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, WebRequestMetricSet> entry : metricsMap.entrySet()) {
            metricsMap.compute(entry.getKey(),
                               (k,
                                v) -> getAndRemove(v));
            messages.add(messageConverter.from(timestamp, interval, metric));
        }
        return messages;
    }

    private WebRequestMetricSet getAndRemove(WebRequestMetricSet metric) {
        this.metric = metric;
        return null;
    }
}
