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
import org.bithon.server.alerting.common.algorithm.ISmoothAlgorithm;
import org.bithon.server.alerting.common.algorithm.SmoothAlgorithm;
import org.bithon.server.alerting.common.algorithm.SmoothAlgorithmFactory;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesMetric;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 5:21 下午
 */
public class BaselineMetricCacheManager {

    private final LoadingCache<QueryRequest, List<Number>> baselineCache;

    public BaselineMetricCacheManager(IDataSourceApi dataSourceApi) {
        this.baselineCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).build(key -> {
            QueryResponse response = dataSourceApi.timeseriesV3(key);

            TimeSeriesQueryResult baseline = (TimeSeriesQueryResult) response.getData();

            TimeSeriesMetric series = null;
            if (!baseline.getMetrics().isEmpty()) {
                // there's only one metric, get the first metric
                series = baseline.getMetrics().iterator().next();
            }

            List<Number> values = new ArrayList<>(baseline.getCount());
            for (int i = 0; i < baseline.getCount(); i++) {
                values.add(series == null ? 0 : series.get(i));
            }

            ISmoothAlgorithm smoothAlgorithm = SmoothAlgorithmFactory.create(SmoothAlgorithm.MovingAverage);
            return smoothAlgorithm.smooth(values);
        });
    }

    public List<Number> getBaselineMetricsList(TimeSpan start,
                                               TimeSpan end,
                                               int stepSeconds,
                                               String dataSource,
                                               String filterExpression,
                                               QueryField field) {
        QueryRequest request = QueryRequest.builder()
                                           .dataSource(dataSource)
                                           .interval(IntervalRequest.builder()
                                                                    .startISO8601(start.toISO8601())
                                                                    .endISO8601(end.toISO8601())
                                                                    .step(stepSeconds)
                                                                    .build())
                                           .filterExpression(filterExpression)
                                           .fields(Collections.singletonList(field))
                                           .build();
        return this.baselineCache.get(request);
    }
}
