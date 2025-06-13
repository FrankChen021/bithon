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

package org.bithon.server.datasource.query.plan.physical;


import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.result.PipelineQueryResult;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:48 pm
 */
public class MetricQueryStep implements IPhysicalPlan {
    private final String dataSource;
    private final Interval interval;
    private final String filterExpression;
    private final Set<String> groupBy;
    private final HumanReadableDuration offset;
    private final boolean isScalar;

    // Make sure the evaluation is executed ONLY ONCE when the expression is referenced multiple times
    private volatile CompletableFuture<PipelineQueryResult> cachedResponse;

    private MetricQueryStep(Builder builder) {
        this.dataSource = builder.dataSource;
        this.interval = builder.interval;
        this.filterExpression = builder.filterExpression;
        this.groupBy = builder.groupBy;
        this.offset = builder.offset;
        this.isScalar = computeIsScalar();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dataSource;
        private Interval interval;
        private String filterExpression;
        private Set<String> groupBy;
        private HumanReadableDuration offset;

        public Builder dataSource(String dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder interval(Interval interval) {
            this.interval = interval;
            return this;
        }

        public Builder filterExpression(String filterExpression) {
            this.filterExpression = filterExpression;
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

        public MetricQueryStep build() {
            return new MetricQueryStep(this);
        }
    }

    private boolean computeIsScalar() {
        if (CollectionUtils.isNotEmpty(groupBy)) {
            // If GROUP-BY is not empty, obviously it is not a scalar because the result set contains multiple rows for different groups
            return false;
        }

        TimeSpan start = interval.getStartTime();
        TimeSpan end = interval.getEndTime();
        long intervalLength = (end.getMilliseconds() - start.getMilliseconds()) / 1000;
        return interval.getStep() != null && interval.getStep().getSeconds() == intervalLength;
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
                        /*try {

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
                        }*/
                        return null;
                    });
                }
            }
        }
        return cachedResponse;
    }
}
