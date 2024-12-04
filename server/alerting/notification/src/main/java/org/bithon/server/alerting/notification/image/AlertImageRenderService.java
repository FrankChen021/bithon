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

package org.bithon.server.alerting.notification.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.notification.NotificationModuleEnabler;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.metric.expression.api.MetricQueryApi;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/8
 */
@Slf4j
@Service
@Conditional(NotificationModuleEnabler.class)
public class AlertImageRenderService implements ApplicationContextAware {

    private final ThreadPoolExecutor executor;
    private final RenderingConfig renderingConfig;
    private final ObjectMapper objectMapper;
    private ApplicationContext applicationContext;

    public AlertImageRenderService(RenderingConfig renderingConfig, ObjectMapper objectMapper) {
        this.renderingConfig = renderingConfig;
        this.objectMapper = objectMapper;
        this.executor = new ThreadPoolExecutor(10,
                                               50,
                                               5,
                                               TimeUnit.MINUTES,
                                               new LinkedBlockingQueue<>(100),
                                               NamedThreadFactory.nonDaemonThreadFactory("image-render-%d"));
        this.executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public boolean isEnabled() {
        return renderingConfig.isEnabled();
    }

    /**
     * render expressions in base64 image format
     */
    public Map<String, String> render(ImageMode imageMode,
                                      AlertRule rule,
                                      List<AlertExpression> expressions,
                                      TimeSpan endExclusive) {
        if (CollectionUtils.isEmpty(expressions)) {
            return Collections.emptyMap();
        }

        Map<String, String> images = new LinkedHashMap<>();

        CountDownLatch latch = new CountDownLatch(expressions.size());
        for (int i = 0, expressionsLength = expressions.size(); i < expressionsLength; i++) {
            final int index = i;
            AlertExpression expression = expressions.get(i);
            this.executor.submit(() -> {
                try {
                    TimeSpan evaluationStart = endExclusive.before(expression.getMetricExpression().getWindow().getDuration());

                    TimeSpan alertingStart = evaluationStart.before(rule.getEvery().getDuration().getSeconds() * (rule.getForTimes() - 1), TimeUnit.SECONDS);

                    // TODO: change to configuration
                    TimeSpan start = evaluationStart.before(Duration.ofHours(1));

                    String image = render(expression,
                                          Interval.of(start, endExclusive),
                                          Interval.of(alertingStart, evaluationStart));

                    images.put(expression.getId(), image);
                } catch (Exception e) {
                    log.error(StringUtils.format("exception when render image, Alert=[%s], Condition=[%s]", rule.getName(), expression.getId()), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return images;
    }

    private String render(AlertExpression expression,
                          Interval visualInterval,
                          Interval evaluationInterval) throws Exception {
        // Get the data for visualization first
        //
        // DO NOT inject the DataSourceApi in ctor
        // See: https://github.com/FrankChen021/bithon/issues/838
        MetricQueryApi metricQueryApi = this.applicationContext.getBean(MetricQueryApi.class);
        QueryResponse response = metricQueryApi.timeSeries(MetricQueryApi.MetricQueryRequest.builder()
                                                                                            .expression(expression.getMetricExpression().serializeToText(true))
                                                                                            .interval(IntervalRequest.builder()
                                                                                                                     .startISO8601(visualInterval.getStartTime().toISO8601())
                                                                                                                     .endISO8601(visualInterval.getEndTime().toISO8601())
                                                                                                                     .step((int) expression.getMetricExpression().getWindow().getDuration().getSeconds())
                                                                                                                     .build())
                                                                                            .build());

        // Render image
        return invokeRenderApi(RenderImageRequest.builder()
                                                 .height(renderingConfig.getHeight())
                                                 .width(renderingConfig.getWidth())
                                                 .expression(expression)
                                                 .data(response)
                                                 .markers(new MarkerSpec[]{
                                                     MarkerSpec.builder()
                                                               .start(evaluationInterval.getStartTime().getMilliseconds())
                                                               .end(evaluationInterval.getEndTime().getMilliseconds())
                                                         .build()
                                                 })
                                                 .build());
    }


    @Data
    @Builder
    static class MarkerSpec {
        long start;
        long end;
    }

    @Data
    @Builder
    static class RenderImageRequest {
        private int height;
        private int width;
        private AlertExpression expression;
        private QueryResponse data;
        private MarkerSpec[] markers;
    }

    @Data
    static class RenderImageResponse {
        private String image;
    }

    private String invokeRenderApi(RenderImageRequest request) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                                                   .setSocketTimeout(30_000)
                                                   .setConnectTimeout(1000)
                                                   .build();
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            HttpPost httpRequest = new HttpPost(this.renderingConfig.getServiceEndpoint());

            httpRequest.setHeader("Content-Type", "application/json");

            // Body
            httpRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(request), StandardCharsets.UTF_8));

            HttpResponse response = client.execute(httpRequest);
            if (response.getStatusLine().getStatusCode() >= 400) {
                throw new RuntimeException(StringUtils.format("Failed to send message to [%s]: Received status: %d, response: %s",
                                                              this.renderingConfig.getServiceEndpoint(),
                                                              response.getStatusLine().getStatusCode(),
                                                              IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8)));
            }
            return this.objectMapper.readValue(response.getEntity().getContent(), RenderImageResponse.class).image;
        }
    }

}
