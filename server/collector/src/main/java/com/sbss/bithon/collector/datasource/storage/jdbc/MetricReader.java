package com.sbss.bithon.collector.datasource.storage.jdbc;

import com.sbss.bithon.collector.datasource.DataSourceSchema;
import com.sbss.bithon.collector.datasource.storage.DimensionCondition;
import com.sbss.bithon.collector.datasource.storage.IMetricReader;
import com.sbss.bithon.collector.common.utils.datetime.TimeSpan;
import com.sbss.bithon.collector.common.matcher.*;
import com.sbss.bithon.component.db.jooq.Tables;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
class MetricReader implements IMetricReader {
    private final DSLContext dsl;

    public MetricReader(DSLContext dsl) {
        this.dsl = dsl;

        if (dsl.configuration().dialect().equals(SQLDialect.H2)) {
            dsl.createTableIfNotExists(Tables.BITHON_JVM_METRICS)
                .columns(Tables.BITHON_JVM_METRICS.TIMESTAMP,
                         Tables.BITHON_JVM_METRICS.APPNAME,
                         Tables.BITHON_JVM_METRICS.INSTANCENAME,
                         Tables.BITHON_JVM_METRICS.PROCESSCPULOAD,
                         Tables.BITHON_JVM_METRICS.INSTANCEUPTIME,
                         Tables.BITHON_JVM_METRICS.INSTANCESTARTTIME,
                         Tables.BITHON_JVM_METRICS.HEAP,
                         Tables.BITHON_JVM_METRICS.HEAPCOMMITTED,
                         Tables.BITHON_JVM_METRICS.HEAPINIT,
                         Tables.BITHON_JVM_METRICS.HEAPUSED,
                         Tables.BITHON_JVM_METRICS.PEAKTHREADS,
                         Tables.BITHON_JVM_METRICS.DAEMONTHREADS,
                         Tables.BITHON_JVM_METRICS.TOTALTHREADS,
                         Tables.BITHON_JVM_METRICS.ACTIVETHREADS)
                .execute();
        }
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
                                                        List<DimensionCondition> dimensions,
                                                        List<String> metrics) {
        String condition = dimensions.stream().map(d -> d.getMatcher().accept(new SqlConditionBuilder(d.getDimension()))).collect(Collectors.joining(" AND "));
        String metricList = metrics.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(", "));
        String sql = String.format(
            "SELECT \"timestamp\", %s FROM \"%s\" WHERE %s AND \"timestamp\" >= '%s' AND \"timestamp\" <= '%s' ",
            metricList,
            dataSourceSchema.getName(),
            condition,
            start.toISO8601(),
            end.toISO8601()
        );
        log.info("Excuting {}", sql);
        return getMetricValueList(sql);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<Map<String, Object>> getMetricValueList(String sql) {
        List<Record> records = dsl.fetch(sql);
        return (List<Map<String,Object>>)records.stream().map(record -> {
            Map<String, Object> mapObject = new HashMap<>();
            for (Field field : record.fields()) {
                mapObject.put(field.getName(), record.get(field));
            }
            return mapObject;
        }).collect(Collectors.toList());
    }
}
