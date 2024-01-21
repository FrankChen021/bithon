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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.metric.absolute.AbstractAbsoluteThresholdPredicate;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.notification.NotificationModuleEnabler;
import org.bithon.server.alerting.notification.message.ImageMode;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.web.service.datasource.api.GeneralQueryRequest;
import org.bithon.server.web.service.datasource.api.GeneralQueryResponse;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.TimeSeriesMetric;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
public class AlertImageRenderService {

    private final ThreadPoolExecutor executor;
    private final RenderingConfig renderingConfig;
    private final IDataSourceApi dataSourceApi;
    private final ObjectMapper objectMapper;
    private final IEChartsConverterApi eChartsConverterApi;

    public AlertImageRenderService(RenderingConfig renderingConfig,
                                   IDataSourceApi dataSourceApi,
                                   IEChartsConverterApi converterApi,
                                   ObjectMapper objectMapper) {
        this.renderingConfig = renderingConfig;
        this.eChartsConverterApi = converterApi;
        this.dataSourceApi = dataSourceApi;
        this.objectMapper = objectMapper;
        this.executor = new ThreadPoolExecutor(10,
                                               50,
                                               5,
                                               TimeUnit.MINUTES,
                                               new LinkedBlockingQueue<>(100),
                                               new ThreadFactoryBuilder().setDaemon(true).setNameFormat("image-render-%d").build());
        this.executor.setRejectedExecutionHandler((r, executor1) -> log.error("Image Render Task has been rejected"));
    }

    public boolean isEnabled() {
        return renderingConfig.isEnabled();
    }

    /**
     * @return URL
     */
    public Future<String> renderAndSaveAsync(ImageMode imageMode,
                                             String alert,
                                             AlertExpression expression,
                                             int windowLength,
                                             TimeSpan start,
                                             TimeSpan end) {
        return this.executor.submit(() -> {
            try {
                return renderAndSave(expression, windowLength, start, end);
            } catch (Exception e) {
                log.error(StringUtils.format("exception when render image, Alert=[%s], Condition=[%s]", alert, expression.getId()), e);
                return null;
            }
        });
    }

    public String renderAndSave(AlertExpression expression,
                                int windowLength,
                                TimeSpan start,
                                TimeSpan end) throws IOException {
        if (!(expression.getMetricEvaluator() instanceof AbstractAbsoluteThresholdPredicate)) {
            // only support an absolute threshold
            return null;
        }

        DataSourceSchema schema = this.dataSourceApi.getSchemaByName(expression.getFrom());
        IColumn metricSpec = schema.getColumnByName(expression.getSelect().getName());

        GeneralQueryRequest request = GeneralQueryRequest.builder()
                                                         .interval(IntervalRequest.builder()
                                                                                  .startISO8601(start.before(1, TimeUnit.HOURS).toISO8601())
                                                                                  .endISO8601(end.toISO8601())
                                                                                  .build())
                                                         .dataSource(expression.getFrom())
                                                         .filterExpression(expression.getWhere())
                                                         .fields(Collections.singletonList(expression.getSelect()))
                                                         .build();
        GeneralQueryResponse response = this.dataSourceApi.timeseriesV3(request);

        TimeSeriesQueryResult data = (TimeSeriesQueryResult) response.getData();
        Number threshold = metricSpec.getDataType().scaleTo((Number) ((AbstractAbsoluteThresholdPredicate) expression.getMetricEvaluator()).getExpected(), 2);

        TimeSeriesMetric metricValues = data.getMetrics().iterator().next();

        // scale the double values to precision of 2, and find the max value
        Number max = threshold;
        List<Number> values = new ArrayList<>(data.getCount());
        for (int i = 0, len = data.getCount(); i < len; i++) {
            Number value = metricSpec.getDataType().scaleTo(metricValues.get(i), 2);
            if (metricSpec.getDataType().isGreaterThan(value, max)) {
                max = value;
            }
            values.add(value);
        }

        try (InputStream is = this.getClass().getResourceAsStream("/templates/static-threshold-echarts-option.js")) {
            if (is == null) {
                return null;
            }
            String template = IOUtils.toString(is, StandardCharsets.UTF_8);
            template = template.replaceAll("%xAxisLabelData%",
                                           objectMapper.writeValueAsString(getTimestampLabels(data, "HH:mm")));
            template = template.replaceAll("%yLabel%", metricSpec.getName() + "(%unit%)");
            template = template.replaceAll("%yMax%", max.toString());
            template = template.replaceAll("%seriesName%", metricSpec.getName());
            template = template.replaceAll("%SeriesData%", objectMapper.writeValueAsString(values));

            //mark line
            template = template.replaceAll("%threshold%", threshold.toString());
            template = template.replaceAll("%startPoint%", end.before(windowLength, TimeUnit.MINUTES).format("HH:mm"));
            template = template.replaceAll("%endPoint%", end.before(1, TimeUnit.MINUTES).format("HH:mm"));
            JsonNode eChartOption = objectMapper.readTree(template);

            IEChartsConverterApi.Response apiResponse = this.eChartsConverterApi.convertAndSave(IEChartsConverterApi.Request.builder()
                                                                                                                            .height(this.renderingConfig.getHeight())
                                                                                                                            .width(this.renderingConfig.getWidth())
                                                                                                                            .eChartOption(eChartOption).build());

            return apiResponse.getUrl();
        }
    }

    private String getTimestampLabels(TimeSeriesQueryResult data, String format) {
        return new SimpleDateFormat(format, Locale.ENGLISH).format(new Date(data.getStartTimestamp()));
    }

    @Configuration
    @EnableFeignClients
    @Import(FeignClientsConfiguration.class)
    public static class RpcAutoConfiguration {
        @Bean
        public IEChartsConverterApi echartsConverterApi(Contract contract,
                                                        Encoder encoder,
                                                        Decoder decoder,
                                                        RenderingConfig renderingConfig) {
            return renderingConfig.isEnabled() ? Feign.builder()
                                                      .contract(contract)
                                                      .encoder(encoder)
                                                      .decoder(decoder)
                                                      .target(IEChartsConverterApi.class, renderingConfig.getServiceEndpoint())
                : request -> null;
        }
    }

}
