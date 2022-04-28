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
import org.bithon.server.alerting.common.algorithm.BaselineCalibrator;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.bithon.server.web.service.api.TimeSeriesQueryRequest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 5:21 下午
 */
@Service
@ConditionalOnBean(AlertingModule.class)
public class BaselineMetricCacheManager {

    private final LoadingCache<TimeSeriesQueryRequest, List<Map<String, Object>>> baselineCache;

    public BaselineMetricCacheManager(IDataSourceApi dataSourceApi) {
        this.baselineCache = Caffeine.newBuilder()
                                     .expireAfterWrite(Duration.ofDays(1))
                                     .build(new CacheLoader<TimeSeriesQueryRequest, List<Map<String, Object>>>() {
                                         @Override
                                         public @Nullable List<Map<String, Object>> load(@NonNull TimeSeriesQueryRequest key) {
                                             List<Map<String, Object>> baseline = dataSourceApi.timeseries0(key);
                                             return new BaselineCalibrator().calibrate(baseline, 1/* min */, key.getMetrics().get(0));
                                         }
                                     });
    }

    public List<Map<String, Object>> getBaselineMetricsList(TimeSpan start,
                                                            TimeSpan end,
                                                            String dataSource,
                                                            List<IFilter> dimensions,
                                                            String metricSpec) {
        TimeSeriesQueryRequest request = TimeSeriesQueryRequest.builder()
                                                               .dataSource(dataSource)
                                                               .startTimeISO8601(start.toISO8601())
                                                               .endTimeISO8601(end.toISO8601())
                                                               .filters(dimensions)
                                                               .metrics(Collections.singletonList(metricSpec))
                                                               .build();
        return this.baselineCache.get(request);
    }
}
