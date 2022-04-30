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

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.bithon.server.alerting.common.AlertingModule;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.web.service.api.DataSourceService;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.bithon.server.web.service.api.IntervalRequest;
import org.bithon.server.web.service.api.TimeSeriesQueryRequestV2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 5:21 下午
 */
@Service
@ConditionalOnBean(AlertingModule.class)
public class BaselineMetricCacheManager {

    private final LoadingCache<TimeSeriesQueryRequestV2, List<Map<String, Object>>> baselineCache;

    public BaselineMetricCacheManager(IDataSourceApi dataSourceApi) {
        this.baselineCache = Caffeine.newBuilder()
                                     .expireAfterWrite(Duration.ofDays(1))
                                     .build(new CacheLoader<TimeSeriesQueryRequestV2, List<Map<String, Object>>>() {
                                         @Override
                                         public @Nullable List<Map<String, Object>> load(@NonNull TimeSeriesQueryRequestV2 key) {
                                             DataSourceService.TimeSeriesQueryResult baseline = dataSourceApi.timeseries(key);

                                             /*
                                             return new BaselineCalibrator().calibrate(baseline.getMetrics().stream().findFirst().get(),
                                                                                       1, // 1 minute
                                                                                       key.getAggregators().get(0).getName());

                                              */
                                             return null;
                                         }
                                     });
    }

    public List<Map<String, Object>> getBaselineMetricsList(TimeSpan start,
                                                            TimeSpan end,
                                                            String dataSource,
                                                            List<IFilter> dimensions,
                                                            String metricSpec) {
        TimeSeriesQueryRequestV2 request = TimeSeriesQueryRequestV2.builder()
                                                                   .dataSource(dataSource)
                                                                   .interval(IntervalRequest.builder()
                                                                                            .startISO8601(start.toISO8601())
                                                                                            .endISO8601(end.toISO8601())
                                                                                            .build())
                                                                   .filters(dimensions)
                                                                   //.metrics(Collections.singletonList(metricSpec))
                                                                   .build();
        return this.baselineCache.get(request);
    }
}
