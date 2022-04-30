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
import org.bithon.server.alerting.processor.notification.NotificationConfig;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.spec.IMetricSpec;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.bithon.server.web.service.api.TimeSeriesQueryRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/8
 */
@Slf4j
@Service
@ConditionalOnBean(AlertingModule.class)
public class AlertImageRenderService {

    private final ThreadPoolExecutor executor;
    private final NotificationConfig renderConfig;
    private final IDataSourceApi dataSourceApi;
    private final ObjectMapper objectMapper;
    private final Map<String, IEChartsConverterApi> eChartsConverterApi;
    private final Contract contract;
    private final Encoder encoder;
    private final Decoder decode;

    public AlertImageRenderService(NotificationConfig notificationConfig,
                                   IDataSourceApi dataSourceApi,
                                   ObjectMapper objectMapper,
                                   Contract contract,
                                   Encoder encoder,
                                   Decoder decode) {
        this.renderConfig = notificationConfig;
        this.dataSourceApi = dataSourceApi;
        this.objectMapper = objectMapper;
        this.contract = contract;
        this.encoder = encoder;
        this.decode = decode;
        this.eChartsConverterApi = new ConcurrentHashMap<>();
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

        TimeSeriesQueryRequest request = TimeSeriesQueryRequest.builder()
                                                               .startTimeISO8601(start.before(1, TimeUnit.HOURS).toISO8601())
                                                               .endTimeISO8601(end.toISO8601())
                                                               .dataSource(condition.getDataSource())
                                                               .filters(condition.getDimensions())
                                                               .metrics(Collections.singletonList(metricSpec.getName()))
                                                               .build();
        //TODO: fix
        List<Map<String, Object>> data = null; //this.dataSourceApi.timeseries0(request);

        Number threshold = metricSpec.getValueType().scaleTo((Number) ((AbstractAbsoluteThresholdCondition) condition.getMetric()).getExpected(), 2);

        class MaxHolder {
            Number max;
        }
        final MaxHolder maxHolder = new MaxHolder();
        maxHolder.max = threshold;
        List<Object> metricValueList = data.stream().map(obj -> {
            Number metricValue = metricSpec.getValueType().scaleTo((Number) obj.get(metricSpec.getName()), 2);
            if (metricSpec.getValueType().isGreaterThan(metricValue, maxHolder.max)) {
                maxHolder.max = metricValue;
            }
            return metricValue;
        }).collect(Collectors.toList());

        InputStream is = this.getClass().getResourceAsStream("/templates/static-threshold-echarts-option.js");
        String template = IOUtils.toString(is);
        template = template.replaceAll("%xAxisLabelData%",
                                       objectMapper.writeValueAsString(data.stream().map(obj -> obj.get("__time")).collect(Collectors.toList())));
        template = template.replaceAll("%yLabel%", metricSpec.getDisplayText() + "(" + metricSpec.getUnit() + ")");
        template = template.replaceAll("%yMax%", maxHolder.max.toString());
        template = template.replaceAll("%seriesName%", metricSpec.getDisplayText());
        template = template.replaceAll("%SeriesData%", objectMapper.writeValueAsString(metricValueList));

        //mark line
        template = template.replaceAll("%threshold%", threshold.toString());
        template = template.replaceAll("%startPoint%", end.before(windowLength, TimeUnit.MINUTES).toString("HH:mm"));
        template = template.replaceAll("%endPoint%", end.before(1, TimeUnit.MINUTES).toString("HH:mm"));
        JsonNode eChartOption = objectMapper.readTree(template);

        IEChartsConverterApi api = eChartsConverterApi.computeIfAbsent(this.renderConfig.getRenderConfig().getRenderService(),
                                                                       url -> echartsConverterApi(this.contract, this.encoder, this.decode, url));
        IEChartsConverterApi.Response response = api.convertAndSave(IEChartsConverterApi.Request.builder()
                                                                                                .height(this.renderConfig.getRenderConfig().getHeight())
                                                                                                .width(this.renderConfig.getRenderConfig().getWidth())
                                                                                                .eChartOption(eChartOption).build());

        return response.getUrl();
    }

    private IEChartsConverterApi echartsConverterApi(Contract contract,
                                                     Encoder encoder,
                                                     Decoder decoder,
                                                     String url) {
        return Feign.builder()
                    .contract(contract)
                    .encoder(encoder)
                    .decoder(decoder)
                    .target(IEChartsConverterApi.class, url);
    }
}
