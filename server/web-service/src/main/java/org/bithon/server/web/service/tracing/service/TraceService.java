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

package org.bithon.server.web.service.tracing.service;

import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ArrayAccessExpression;
import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor2;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.filter.IColumnFilter;
import org.bithon.server.storage.datasource.query.Order;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.index.TagIndexConfig;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.common.bucket.TimeBucket;
import org.bithon.server.web.service.datasource.api.FilterExpressionToFilters;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;
import org.bithon.server.web.service.tracing.api.TraceSpanBo;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 24/11/21 7:11 pm
 */
@Service
@Conditional(value = WebServiceModuleEnabler.class)
public class TraceService {

    private final ITraceReader traceReader;
    private final TraceStorageConfig traceStorageConfig;
    private final DataSourceSchema traceSpanSummarySchema;

    public TraceService(ITraceStorage traceStorage,
                        TraceStorageConfig traceStorageConfig,
                        DataSourceSchemaManager dataSourceSchemaManager) {
        this.traceReader = traceStorage.createReader();
        this.traceStorageConfig = traceStorageConfig;
        this.traceSpanSummarySchema = dataSourceSchemaManager.getDataSourceSchema("trace_span_summary");
    }

    public List<TraceSpan> getTraceByParentSpanId(String parentSpanId) {
        return traceReader.getTraceByParentSpanId(parentSpanId);
    }

    public List<TraceSpan> getTraceByTraceId(String txId,
                                             String type,
                                             String startTimeISO8601,
                                             String endTimeISO8601) {
        TimeSpan start = StringUtils.hasText(startTimeISO8601) ? TimeSpan.fromISO8601(startTimeISO8601) : null;
        TimeSpan end = StringUtils.hasText(endTimeISO8601) ? TimeSpan.fromISO8601(endTimeISO8601) : null;

        if (!"trace".equals(type)) {
            // check if the id has a user mapping
            TraceIdMapping mapping = traceReader.getTraceIdByMapping(txId);
            if (mapping != null) {
                txId = mapping.getTraceId();

                // Set the time range to narrow down the search range
                if (start == null) {
                    start = TimeSpan.of(mapping.getTimestamp() - 2 * 3600 * 1000L);
                }
                if (end == null) {
                    end = TimeSpan.of(mapping.getTimestamp() + 2 * 3600 * 1000L);
                }
            }
            // if there's no mapping, try to search this id as trace id
        }

        return traceReader.getTraceByTraceId(txId, start, end);
    }

    public List<TraceSpan> asTree(List<TraceSpan> spans) {
        Map<String, TraceSpanBo> boMap = spans.stream().collect(Collectors.toMap(span -> span.spanId, val -> {
            TraceSpanBo bo = new TraceSpanBo();
            BeanUtils.copyProperties(val, bo);
            return bo;
        }));

        List<TraceSpan> rootSpans = new ArrayList<>();
        for (TraceSpan span : spans) {
            TraceSpanBo bo = boMap.get(span.spanId);
            if (StringUtils.isEmpty(span.parentSpanId)) {
                rootSpans.add(bo);
            } else {
                TraceSpanBo parentSpan = boMap.get(span.parentSpanId);
                if (parentSpan == null) {
                    // For example, two applications: A --> B
                    // if span logs of A are not stored in Bithon,
                    // the root span of B has parentSpanId, but apparently the parent span can't be found
                    rootSpans.add(bo);
                } else {
                    parentSpan.children.add(bo);
                }
            }
        }
        return rootSpans;
    }

    public int getTraceListSize(List<IColumnFilter> filters,
                                String filterExpression,
                                Timestamp start,
                                Timestamp end) {
        FilterSplitter splitter = new FilterSplitter(this.traceStorageConfig);
        splitter.split(FilterExpressionToFilters.toExpression(traceSpanSummarySchema, filterExpression, filters));

        return traceReader.getTraceListSize(splitter.expression,
                                            splitter.nonIndexedTagFilters,
                                            splitter.indexedTagFilters,
                                            start,
                                            end);
    }

    public List<TraceSpan> getTraceList(List<IColumnFilter> filters,
                                        String filterExpression,
                                        Timestamp start,
                                        Timestamp end,
                                        String orderBy,
                                        Order order,
                                        int pageNumber,
                                        int pageSize) {
        FilterSplitter splitter = new FilterSplitter(this.traceStorageConfig);
        splitter.split(FilterExpressionToFilters.toExpression(traceSpanSummarySchema, filterExpression, filters));

        return traceReader.getTraceList(splitter.expression,
                                        splitter.nonIndexedTagFilters,
                                        splitter.indexedTagFilters,
                                        start,
                                        end,
                                        orderBy,
                                        order,
                                        pageNumber, pageSize);
    }

    public TimeSeriesQueryResult getTraceDistribution(List<IColumnFilter> filters,
                                                      String filterExpression,
                                                      TimeSpan start,
                                                      TimeSpan end,
                                                      int bucketCount) {
        FilterSplitter splitter = new FilterSplitter(this.traceStorageConfig);
        splitter.split(FilterExpressionToFilters.toExpression(this.traceSpanSummarySchema, filterExpression, filters));

        int interval = TimeBucket.calculate(start.getMilliseconds(), end.getMilliseconds(), bucketCount).getLength();
        List<Map<String, Object>> dataPoints = traceReader.getTraceDistribution(splitter.expression,
                                                                                splitter.nonIndexedTagFilters,
                                                                                splitter.indexedTagFilters,
                                                                                start.toTimestamp(),
                                                                                end.toTimestamp(),
                                                                                interval);
        List<String> metrics = Arrays.asList("count", "minResponse", "avgResponse", "maxResponse");
        return TimeSeriesQueryResult.build(start,
                                           end,
                                           interval,
                                           dataPoints,
                                           "_timestamp",
                                           Collections.emptyList(),
                                           metrics);
    }

    static class FilterSplitter {
        private final TraceStorageConfig traceStorageConfig;
        private IExpression expression;
        private Map<Integer, IExpression> indexedTagFilters;
        private List<IExpression> nonIndexedTagFilters;

        public FilterSplitter(TraceStorageConfig traceStorageConfig) {
            this.traceStorageConfig = traceStorageConfig;
        }

        void split(IExpression expression) {
            if (expression == null) {
                return;
            }

            if (!(expression instanceof LogicalExpression)
                && !(expression instanceof ComparisonExpression)
                && !(expression instanceof BinaryExpression.In)
                && !(expression instanceof BinaryExpression.Like)
            ) {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                                "The given expression is neither a logical expression(and/or/not) nor a comparison expression, but a %s",
                                                expression.getType());
            }

            TagFilterExtractor extractor = new TagFilterExtractor(this.traceStorageConfig);
            expression.accept(extractor);
            this.indexedTagFilters = extractor.indexedTagFilter;
            this.nonIndexedTagFilters = extractor.nonIndexedTagFilter;
            this.expression = expression;
        }
    }

    static class TagFilterExtractor implements IExpressionVisitor2<Boolean> {

        private int currentTagIndex = -1;

        private final Map<Integer, IExpression> indexedTagFilter;
        private final List<IExpression> nonIndexedTagFilter;

        private final TagIndexConfig indexedTagConfig;

        public TagFilterExtractor(TraceStorageConfig traceStorageConfig) {
            this.indexedTagFilter = new TreeMap<>();
            this.nonIndexedTagFilter = new ArrayList<>();
            indexedTagConfig = traceStorageConfig.getIndexes();
        }

        @Override
        public Boolean visit(ExpressionList expression) {
            return false;
        }

        @Override
        public Boolean visit(ArrayAccessExpression expression) {
            return false;
        }

        @Override
        public Boolean visit(ArithmeticExpression expression) {
            return false;
        }

        @Override
        public Boolean visit(MacroExpression expression) {
            return false;
        }

        @Override
        public Boolean visit(FunctionExpression expression) {
            return false;
        }

        @Override
        public Boolean visit(ComparisonExpression expression) {
            IExpression left = expression.getLeft();
            if (left instanceof IdentifierExpression) {
                return isFilterOnIndexedTag((IdentifierExpression) left);
            }
            IExpression right = expression.getRight();
            if (right instanceof IdentifierExpression) {
                return isFilterOnIndexedTag((IdentifierExpression) right);
            }
            return false;
        }

        @Override
        public Boolean visit(BinaryExpression expression) {
            IExpression left = expression.getLeft();
            if (left instanceof IdentifierExpression) {
                return isFilterOnIndexedTag((IdentifierExpression) left);
            }
            IExpression right = expression.getRight();
            if (right instanceof IdentifierExpression) {
                return isFilterOnIndexedTag((IdentifierExpression) right);
            }
            return false;
        }

        @Override
        public Boolean visit(LogicalExpression expression) {
            if (!(expression instanceof LogicalExpression.AND)) {
                throw new RuntimeException("Only AND operator is supported to search tracing.");
            }

            List<IExpression> nonTagsFilters = new ArrayList<>();
            for (IExpression subExpression : expression.getOperands()) {
                if (subExpression.accept(this)) {
                    if (currentTagIndex > 0) {
                        indexedTagFilter.put(currentTagIndex, subExpression);
                    } else {
                        nonIndexedTagFilter.add(subExpression);
                    }
                } else {
                    nonTagsFilters.add(subExpression);
                }
            }
            if (nonTagsFilters.isEmpty()) {
                // Add a placeholder expression so simple further processing
                nonTagsFilters.add(new ComparisonExpression.EQ(LiteralExpression.create(1), LiteralExpression.create(1)));
            }
            expression.setOperands(nonTagsFilters);

            return false;
        }

        private boolean isFilterOnIndexedTag(IdentifierExpression expression) {
            String identifier = expression.getIdentifier();
            if (identifier.startsWith("tags.")) {

                String tagName = identifier.substring("tags.".length());
                if (!StringUtils.hasText(tagName)) {
                    throw new RuntimeException(StringUtils.format("Wrong tag name [%s]", identifier));
                }

                currentTagIndex = indexedTagConfig == null ? 0 : indexedTagConfig.getColumnPos(tagName);
                return true;
            }
            return false;
        }
    }
}
