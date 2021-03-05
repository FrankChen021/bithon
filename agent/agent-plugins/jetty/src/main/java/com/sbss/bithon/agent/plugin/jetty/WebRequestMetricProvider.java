package com.sbss.bithon.agent.plugin.jetty;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.web.WebRequestMetric;
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
public class WebRequestMetricProvider implements IMetricProvider {

    private WebRequestMetric metric;

    private final Map<String, WebRequestMetric> metricsMap = new ConcurrentHashMap<>();

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

        WebRequestMetric webRequestMetrics = metricsMap.computeIfAbsent(srcApplication + "|" + uri, key -> new WebRequestMetric(srcApplication, uri));
        webRequestMetrics.add(costTime, errorCount, count4xx, count5xx);
        webRequestMetrics.addBytes(requestByteSize, responseByteSize);
    }

    @Override
    public boolean isEmpty() {
        return metricsMap.isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, WebRequestMetric> entry : metricsMap.entrySet()) {
            metricsMap.compute(entry.getKey(),
                               (k,
                              v) -> getAndRemove(v));
            messages.add(messageConverter.from(appInstance, timestamp, interval, metric));
        }
        return messages;
    }

    private WebRequestMetric getAndRemove(WebRequestMetric metric) {
        this.metric = metric;
        return null;
    }
}
