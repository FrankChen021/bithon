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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.metric.absolute.AbstractAbsoluteThresholdPredicate;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.notification.NotificationModuleEnabler;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.Future;
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

    public boolean isEnabled() {
        return renderingConfig.isEnabled();
    }

    /**
     * @return URL
     */
    public Future<String> render(ImageMode imageMode,
                                 String alert,
                                 AlertExpression expression,
                                 int windowLength,
                                 TimeSpan start,
                                 TimeSpan end) {
        return this.executor.submit(() -> {
            try {
                return render(expression, windowLength, start, end);
            } catch (Exception e) {
                log.error(StringUtils.format("exception when render image, Alert=[%s], Condition=[%s]", alert, expression.getId()), e);
                return null;
            }
        });
    }

    private String render(AlertExpression expression,
                          int windowLength,
                          TimeSpan start,
                          TimeSpan end) throws IOException {
        if (!(expression.getMetricEvaluator() instanceof AbstractAbsoluteThresholdPredicate)) {
            // only support an absolute threshold
            return null;
        }

        // DO NOT inject the DataSourceApi in ctor
        // See: https://github.com/FrankChen021/bithon/issues/838
        IDataSourceApi dataSourceApi = this.applicationContext.getBean(IDataSourceApi.class);
        ISchema schema = dataSourceApi.getSchemaByName(expression.getMetricExpression().getFrom());
        IColumn metricSpec = schema.getColumnByName(expression.getMetricExpression().getMetric().getName());

        QueryRequest request = QueryRequest.builder()
                                           .interval(IntervalRequest.builder()
                                                                    .startISO8601(start.before(1, TimeUnit.HOURS).toISO8601())
                                                                    .endISO8601(end.toISO8601())
                                                                    .build())
                                           .dataSource(expression.getMetricExpression().getFrom())
                                           .filterExpression(expression.getMetricExpression().getWhereText())
                                           .fields(Collections.singletonList(expression.getMetricExpression().getMetric()))
                                           .build();
        QueryResponse response = dataSourceApi.timeseriesV4(request);
        return "";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    static class MarkerSpec {
        long start;
        long end;
    }

    static class RenderExpression {
        private int height;
        private int width;
        private AlertExpression expression;
        private QueryResponse data;
        private MarkerSpec[] markers;
    }

    @Data
    static class RenderImageRequest {
        private RenderExpression[] expressions;
    }

    @Data
    static class RenderImageResponse {
        private String images[];
    }

    private void callRemoteService(RenderImageRequest request) throws IOException {
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
            RenderImageResponse renderResponse = this.objectMapper.readValue(response.getEntity().getContent(), RenderImageResponse.class);
        }
    }

}
