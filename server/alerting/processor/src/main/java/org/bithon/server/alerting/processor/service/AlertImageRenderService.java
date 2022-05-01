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

package org.bithon.server.alerting.processor.service;

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
import org.bithon.server.alerting.common.AlertingModule;
import org.bithon.server.alerting.common.evaluator.metric.absolute.AbstractAbsoluteThresholdCondition;
import org.bithon.server.alerting.common.model.AlertCondition;
import org.bithon.server.alerting.common.notification.ImageMode;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.spec.IMetricSpec;
import org.bithon.server.web.service.api.DataSourceService;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.bithon.server.web.service.api.IntervalRequest;
import org.bithon.server.web.service.api.TimeSeriesQueryRequestV2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
@ConditionalOnBean(AlertingModule.class)
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

    /**
     * @return URL
     */
    public Future<String> renderAndSaveAsync(ImageMode imageMode,
                                             String alert,
                                             AlertCondition condition,
                                             int windowLength,
                                             TimeSpan start,
                                             TimeSpan end) {
        return this.executor.submit(() -> {
            try {
                return renderAndSave(condition, windowLength, start, end);
            } catch (Exception e) {
                log.error(StringUtils.format("exception when render image, Alert=[%s], Condition=[%s]", alert, condition.getId()), e);
                return null;
            }
        });
    }

    public String renderAndSave(AlertCondition condition,
                                int windowLength,
                                TimeSpan start,
                                TimeSpan end) throws IOException {
        if (!(condition.getMetric() instanceof AbstractAbsoluteThresholdCondition)) {
            // only support absolute threshold
            return null;
        }

        DataSourceSchema schema = this.dataSourceApi.getSchemaByName(condition.getDataSource());
        IMetricSpec metricSpec = schema.getMetricSpecByName(condition.getMetric().getName());

        TimeSeriesQueryRequestV2 request = TimeSeriesQueryRequestV2.builder()
                                                                   .interval(IntervalRequest.builder()
                                                                                            .startISO8601(start.before(1, TimeUnit.HOURS).toISO8601())
                                                                                            .endISO8601(end.toISO8601())
                                                                                            .build())
                                                                   .dataSource(condition.getDataSource())
                                                                   .filters(condition.getDimensions())
                                                                   .aggregators(Collections.singletonList(condition.getMetric().createAggregator()))
                                                                   .build();
        DataSourceService.TimeSeriesQueryResult data = this.dataSourceApi.timeseries(request);

        Number threshold = metricSpec.getValueType().scaleTo((Number) ((AbstractAbsoluteThresholdCondition) condition.getMetric()).getExpected(), 2);

        DataSourceService.TimeSeriesMetric metricValues = data.getMetrics().iterator().next();

        // scale the double values to precision of 2, and find the max value
        Number max = threshold;
        List<Number> values = new ArrayList<>(data.getCount());
        for (int i = 0, len = data.getCount(); i < len; i++) {
            Number value = metricSpec.getValueType().scaleTo(metricValues.get(i), 2);
            if (metricSpec.getValueType().isGreaterThan(value, max)) {
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
                                           objectMapper.writeValueAsString(data.getTimestampLabels("HH:mm")));
            template = template.replaceAll("%yLabel%", metricSpec.getDisplayText() + "(" + metricSpec.getUnit() + ")");
            template = template.replaceAll("%yMax%", max.toString());
            template = template.replaceAll("%seriesName%", metricSpec.getDisplayText());
            template = template.replaceAll("%SeriesData%", objectMapper.writeValueAsString(values));

            //mark line
            template = template.replaceAll("%threshold%", threshold.toString());
            template = template.replaceAll("%startPoint%", end.before(windowLength, TimeUnit.MINUTES).toString("HH:mm"));
            template = template.replaceAll("%endPoint%", end.before(1, TimeUnit.MINUTES).toString("HH:mm"));
            JsonNode eChartOption = objectMapper.readTree(template);

            IEChartsConverterApi.Response response = this.eChartsConverterApi.convertAndSave(IEChartsConverterApi.Request.builder()
                                                                                                                         .height(this.renderingConfig.getHeight())
                                                                                                                         .width(this.renderingConfig.getWidth())
                                                                                                                         .eChartOption(eChartOption).build());

            return response.getUrl();
        }
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
