package com.sbss.bithon.server.metric.storage.jdbc;

import com.sbss.bithon.server.common.matcher.*;
import com.sbss.bithon.server.common.utils.datetime.TimeSpan;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.storage.DimensionCondition;
import com.sbss.bithon.server.metric.storage.IMetricReader;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
class MetricJdbcReader implements IMetricReader {
    private final DSLContext dsl;

    public MetricJdbcReader(DSLContext dsl) {
        this.dsl = dsl;
    }

    static class SqlConditionBuilder implements IMatcherVisitor<String> {
        private final String name;

        SqlConditionBuilder(String name) {
            this.name = name;
        }

        @Override
        public String visit(EqualMatcher matcher) {
            StringBuilder sb = new StringBuilder(64);
            sb.append("\"");
            sb.append(name);
            sb.append("\"");
            sb.append("=");
            sb.append('\'');
            sb.append(matcher.getPattern());
            sb.append('\'');
            return sb.toString();
        }

        @Override
        public <T> T visit(AntPathMatcher antPathMatcher) {
            return null;
        }

        @Override
        public <T> T visit(ContainsMatcher containsMatcher) {
            return null;
        }

        @Override
        public <T> T visit(EndwithMatcher endwithMatcher) {
            return null;
        }

        @Override
        public <T> T visit(IContainsMatcher iContainsMatcher) {
            return null;
        }

        @Override
        public <T> T visit(RegexMatcher regexMatcher) {
            return null;
        }

        @Override
        public <T> T visit(StartwithMatcher startwithMatcher) {
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> getMetricValueList(TimeSpan start,
                                                        TimeSpan end,
                                                        DataSourceSchema dataSourceSchema,
                                                        Collection<DimensionCondition> dimensions,
                                                        Collection<String> metrics) {
        String condition = dimensions.stream().map(d -> d.getMatcher().accept(new SqlConditionBuilder(d.getDimension()))).collect(Collectors.joining(" AND "));
        String metricList = metrics.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(", "));
        String sql = String.format(
            "SELECT \"timestamp\", %s FROM \"%s\" WHERE %s AND \"timestamp\" >= '%s' AND \"timestamp\" <= '%s' ",
            metricList,
            "bithon_" + dataSourceSchema.getName().replace("-", "_"),
            condition,
            start.toISO8601(),
            end.toISO8601()
        );
        log.info("Executing {}", sql);
        return getMetricValueList(sql);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<Map<String, Object>> getMetricValueList(String sql) {
        List<Record> records = dsl.fetch(sql);

        // although the explicit cast seems unnecessary, it must be kept so that compilation can pass
        return (List<Map<String, Object>>)records.stream().map(record -> {
            Map<String, Object> mapObject = new HashMap<>();
            for (Field field : record.fields()) {
                mapObject.put(field.getName(), record.get(field));
            }
            return mapObject;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getDimensionValueList(TimeSpan start,
                                                           TimeSpan end,
                                                           DataSourceSchema dataSourceSchema,
                                                           Collection<DimensionCondition> conditions,
                                                           String dimension) {
        String condition = conditions.stream().map(d -> d.getMatcher().accept(new SqlConditionBuilder(d.getDimension()))).collect(Collectors.joining(" AND "));
        String sql = String.format(
            "SELECT DISTINCT(\"%s\") \"%s\" FROM \"%s\" WHERE %s AND \"timestamp\" >= '%s' AND \"timestamp\" <= '%s' ",
            dimension,
            dimension,
            "bithon_" + dataSourceSchema.getName().replace("-", "_"),
            condition,
            start.toISO8601(),
            end.toISO8601()
        );

        log.info("Executing {}", sql);
        List<Record> records = dsl.fetch(sql);
        return records.stream().map(record -> {
            Map<String, String> mapObject = new HashMap<>();
            for (Field field : record.fields()) {
                mapObject.put("value", record.get(field).toString());
            }
            return mapObject;
        }).collect(Collectors.toList());
    }
}
