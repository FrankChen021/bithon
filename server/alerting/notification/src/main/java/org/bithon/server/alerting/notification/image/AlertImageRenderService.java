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
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.utils.Validator;
import org.bithon.server.alerting.notification.NotificationModuleEnabler;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.metric.expression.api.MetricQueryApi;
import org.bithon.server.storage.alerting.Label;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/8
 */
@Slf4j
@Service
@Conditional(NotificationModuleEnabler.class)
public class AlertImageRenderService implements ApplicationContextAware {

    @Data
    @Builder
    public static class EvaluatedExpression {
        private AlertExpression alertExpression;
        private List<Label> labels;
    }

    private final RenderingConfig renderingConfig;
    private final ObjectMapper objectMapper;
    private ApplicationContext applicationContext;

    public AlertImageRenderService(RenderingConfig renderingConfig, ObjectMapper objectMapper) {
        Validator.validate(renderingConfig);
        Preconditions.checkNotNull(renderingConfig.isEnabled() && StringUtils.hasText(renderingConfig.getServiceEndpoint()), "serviceEndpoint MUST NOT be empty");

        this.renderingConfig = renderingConfig;
        this.objectMapper = objectMapper;
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
                                      List<EvaluatedExpression> expressions,
                                      TimeSpan endExclusive) {
        if (CollectionUtils.isEmpty(expressions)) {
            return Collections.emptyMap();
        }

        Map<String, String> images = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> renderTasks = expressions.stream()
                                                               .filter((expression) -> shouldRenderExpression(rule, expression.getAlertExpression().getId()))
                                                               .map((expression) ->
                                                                        CompletableFuture.runAsync(() -> {
                                                                            try {
                                                                                // Start of current evaluation
                                                                                TimeSpan evaluationStart = endExclusive.before(expression.getAlertExpression().getMetricExpression().getWindow().getDuration());

                                                                                // Start of first triggering timestamp
                                                                                TimeSpan alertingStart = evaluationStart.before(rule.getEvery().getDuration().getSeconds() * (rule.getForTimes() - 1), TimeUnit.SECONDS);

                                                                                TimeSpan visualizationStart = evaluationStart.before(Duration.ofHours(renderingConfig.getDataRange()));

                                                                                String image = render(expression,
                                                                                                      Interval.of(visualizationStart, endExclusive),
                                                                                                      Interval.of(alertingStart, evaluationStart));

                                                                                images.put(expression.getAlertExpression().getId(), image);
                                                                            } catch (Exception e) {
                                                                                log.error(StringUtils.format("exception when render image, Alert=[%s], Condition=[%s]", rule.getName(), expression.getAlertExpression().getId()), e);
                                                                            }
                                                                        })).toList();

        // Wait for all tasks to complete
        CompletableFuture.allOf(renderTasks.toArray(new CompletableFuture[0])).join();

        return new TreeMap<>(images);
    }

    private boolean shouldRenderExpression(AlertRule rule, String expressionId) {
        Set<String> renderExpressions = rule.getNotificationProps().getRenderExpressions();
        boolean shouldRender = renderExpressions != null && renderExpressions.contains(expressionId);

        log.debug("Rendering expression [{}] for alert [{}]: {}", expressionId, rule.getName(), shouldRender);

        return shouldRender;
    }

    private String render(EvaluatedExpression expression,
                          Interval visualInterval,
                          Interval evaluationInterval) throws Exception {
        // Get the data for visualization first
        //
        // DO NOT inject the DataSourceApi in ctor
        // See: https://github.com/FrankChen021/bithon/issues/838
        MetricQueryApi metricQueryApi = this.applicationContext.getBean(MetricQueryApi.class);

        IExpression initLabelSelector = expression.getAlertExpression().getMetricExpression().getLabelSelectorExpression();

        //
        // If matched labels are specified, append them in the expression so that we get data for these labels only
        //
        if (!expression.getLabels().isEmpty()) {
            List<IExpression> multipleGroups = new ArrayList<>();
            for (Label label : expression.getLabels()) {

                List<IExpression> oneGroup = new ArrayList<>();
                for (Map.Entry<String, String> entry : label.getKeyValues().entrySet()) {
                    IExpression selector = new ComparisonExpression.EQ(new IdentifierExpression(entry.getKey()),
                                                                       LiteralExpression.StringLiteral.of(entry.getValue()));
                    oneGroup.add(selector);
                }

                multipleGroups.add(new LogicalExpression.AND(oneGroup));
            }

            IExpression labelSelector = expression.getAlertExpression()
                                                  .getMetricExpression()
                                                  .getLabelSelectorExpression();

            // Update the label selector to include data with series that matches the label
            expression.getAlertExpression()
                      .getMetricExpression()
                      .setLabelSelectorExpression(new LogicalExpression.AND(labelSelector, new LogicalExpression.OR(multipleGroups)));
        }

        QueryResponse response = metricQueryApi.timeSeries(MetricQueryApi.MetricQueryRequest.builder()
                                                                                            .expression(expression.getAlertExpression().getMetricExpression().serializeToText(true))
                                                                                            .interval(IntervalRequest.builder()
                                                                                                                     .startISO8601(visualInterval.getStartTime().toISO8601())
                                                                                                                     .endISO8601(visualInterval.getEndTime().toISO8601())
                                                                                                                     .step((int) expression.getAlertExpression()
                                                                                                                                           .getMetricExpression()
                                                                                                                                           .getWindow()
                                                                                                                                           .getDuration()
                                                                                                                                           .getSeconds())
                                                                                                                     .build())
                                                                                            .build());

        // Restore the label selector
        expression.getAlertExpression().getMetricExpression().setLabelSelectorExpression(initLabelSelector);

        // Render image
        return invokeRenderApi(RenderImageRequest.builder()
                                                 .height(renderingConfig.getHeight())
                                                 .width(renderingConfig.getWidth())
                                                 .expression(expression.getAlertExpression())
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
