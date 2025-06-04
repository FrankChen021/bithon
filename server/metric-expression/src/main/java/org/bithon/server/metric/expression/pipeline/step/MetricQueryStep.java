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

package org.bithon.server.metric.expression.pipeline.step;


import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.query.plan.physical.ColumnarTable;
import org.bithon.server.datasource.query.plan.physical.IQueryStep;
import org.bithon.server.datasource.query.plan.physical.PipelineQueryResult;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:48 pm
 */
public class MetricQueryStep implements IQueryStep {
    private final String dataSource;
    private final IntervalRequest interval;
    private final String filterExpression;
    private final List<QueryField> fields;
    private final Set<String> groupBy;
    private final HumanReadableDuration offset;
    private final IDataSourceApi dataSourceApi;
    private final boolean isScalar;

    // Make sure the evaluation is executed ONLY ONCE when the expression is referenced multiple times
    private volatile CompletableFuture<PipelineQueryResult> cachedResponse;

    private MetricQueryStep(Builder builder) {
        this.dataSource = builder.dataSource;
        this.interval = builder.interval;
        this.filterExpression = builder.filterExpression;
        this.fields = builder.fields;
        this.groupBy = builder.groupBy;
        this.offset = builder.offset;
        this.dataSourceApi = builder.dataSourceApi;
        this.isScalar = computeIsScalar();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dataSource;
        private IntervalRequest interval;
        private String filterExpression;
        private List<QueryField> fields;
        private Set<String> groupBy;
        private HumanReadableDuration offset;
        private IDataSourceApi dataSourceApi;

        public Builder dataSource(String dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder interval(IntervalRequest interval) {
            this.interval = interval;
            return this;
        }

        public Builder filterExpression(String filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        public Builder fields(List<QueryField> fields) {
            this.fields = fields;
            return this;
        }

        public Builder groupBy(Set<String> groupBy) {
            this.groupBy = groupBy;
            return this;
        }

        public Builder offset(HumanReadableDuration offset) {
            this.offset = offset;
            return this;
        }

        public Builder dataSourceApi(IDataSourceApi dataSourceApi) {
            this.dataSourceApi = dataSourceApi;
            return this;
        }

        public MetricQueryStep build() {
            return new MetricQueryStep(this);
        }
    }

    private boolean computeIsScalar() {
        if (CollectionUtils.isNotEmpty(groupBy)) {
            // If GROUP-BY is not empty, obviously it is not a scalar because the result set contains multiple rows for different groups
            return false;
        }

        if (interval.getBucketCount() != null && interval.getBucketCount() == 1) {
            // ONLY one bucket is requested, the result set is a scalar
            return true;
        }

        TimeSpan start = interval.getStartISO8601();
        TimeSpan end = interval.getEndISO8601();
        long intervalLength = (end.getMilliseconds() - start.getMilliseconds()) / 1000;
        return interval.getStep() != null && interval.getStep() == intervalLength;
    }

    @Override
    public boolean isScalar() {
        return isScalar;
    }

    @Override
    public CompletableFuture<PipelineQueryResult> execute() {
        if (cachedResponse == null) {
            synchronized (this) {
                if (cachedResponse == null) {
                    cachedResponse = CompletableFuture.supplyAsync(() -> {
                        try {
                            QueryRequest queryRequest = QueryRequest.builder()
                                                                    .dataSource(dataSource)
                                                                    .interval(interval)
                                                                    .filterExpression(filterExpression)
                                                                    .fields(fields)
                                                                    .groupBy(groupBy)
                                                                    .offset(offset)
                                                                    .build();

                            ColumnarTable columnTable = dataSourceApi.timeseriesV5(queryRequest);

                            List<String> keys = new ArrayList<>();
                            keys.add(TimestampSpec.COLUMN_ALIAS);
                            keys.addAll(groupBy != null ? groupBy : Collections.emptySet());

                            List<String> valNames = fields.stream()
                                                          .map(QueryField::getName)
                                                          .toList();

                            return PipelineQueryResult.builder()
                                                      .rows(columnTable.rowCount())
                                                      .keyColumns(keys)
                                                      .valColumns(valNames)
                                                      .table(columnTable)
                                                      .build();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
        return cachedResponse;
    }
}
