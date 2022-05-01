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

package org.bithon.server.alerting.common.evaluator.metric.relative.baseline;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.bithon.server.alerting.common.AlertingModule;
import org.bithon.server.alerting.common.algorithm.ISmoothAlgorithm;
import org.bithon.server.alerting.common.algorithm.SmoothAlgorithm;
import org.bithon.server.alerting.common.algorithm.SmoothAlgorithmFactory;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.web.service.api.DataSourceService;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.bithon.server.web.service.api.IntervalRequest;
import org.bithon.server.web.service.api.TimeSeriesQueryRequestV2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 5:21 下午
 */
@Service
@ConditionalOnBean(AlertingModule.class)
public class BaselineMetricCacheManager {

    private final LoadingCache<TimeSeriesQueryRequestV2, List<Number>> baselineCache;

    public BaselineMetricCacheManager(IDataSourceApi dataSourceApi) {
        this.baselineCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).build(key -> {
            DataSourceService.TimeSeriesQueryResult baseline = dataSourceApi.timeseries(key);

            DataSourceService.TimeSeriesMetric series = baseline.getMetrics().iterator().next();

            List<Number> values = new ArrayList<>();
            for (int i = 0; i < baseline.getCount(); i++) {
                values.add(series.get(i));
            }

            ISmoothAlgorithm smoothAlogrithm = SmoothAlgorithmFactory.create(SmoothAlgorithm.MovingAverage);
            return smoothAlogrithm.smooth(values);
        });
    }

    public List<Number> getBaselineMetricsList(TimeSpan start,
                                               TimeSpan end,
                                               int stepSeconds,
                                               String dataSource,
                                               List<IFilter> dimensions,
                                               IQueryStageAggregator aggregator) {
        TimeSeriesQueryRequestV2 request = TimeSeriesQueryRequestV2.builder()
                                                                   .dataSource(dataSource)
                                                                   .interval(IntervalRequest.builder()
                                                                                            .startISO8601(start.toISO8601())
                                                                                            .endISO8601(end.toISO8601())
                                                                                            .step(stepSeconds)
                                                                                            .build())
                                                                   .filters(dimensions)
                                                                   .aggregators(Collections.singletonList(aggregator))
                                                                   .build();
        return this.baselineCache.get(request);
    }
}
