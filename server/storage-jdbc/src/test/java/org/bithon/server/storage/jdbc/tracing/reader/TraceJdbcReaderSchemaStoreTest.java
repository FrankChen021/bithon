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

package org.bithon.server.storage.jdbc.tracing.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.server.commons.time.Period;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.column.LongColumn;
import org.bithon.server.datasource.column.ObjectColumn;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.WindowFunctionExpression;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceJdbcReaderSchemaStoreTest {

    private static final String CUSTOM_TRACE_SPAN_STORE = "custom_trace_span";

    @Test
    void getTraceByTraceIdUsesTraceSpanSchemaStore() throws Exception {
        List<String> sqls = new ArrayList<>();

        newReader(sqls).getTraceByTraceId("trace-1", null, null, null).close();

        assertUsesCustomTraceSpanStore(sqls.get(0));
    }

    @Test
    void getTraceSpanCountUsesTraceSpanSchemaStore() {
        List<String> sqls = new ArrayList<>();

        newReader(sqls).getTraceSpanCount("trace-1", null, null, null);

        assertUsesCustomTraceSpanStore(sqls.get(0));
    }

    @Test
    void getTraceByParentSpanIdUsesTraceSpanSchemaStore() {
        List<String> sqls = new ArrayList<>();

        newReader(sqls).getTraceByParentSpanId("span-1");

        assertUsesCustomTraceSpanStore(sqls.get(0));
    }

    @Test
    void getTraceSpanDistributionUsesTraceSpanSchemaStore() {
        List<String> sqls = new ArrayList<>();

        newReader(sqls).getTraceSpanDistribution("trace-1", null, null, null, List.of("appName"));

        assertUsesCustomTraceSpanStore(sqls.get(0));
    }

    private static TraceJdbcReader newReader(List<String> sqls) {
        DSLContext dslContext = DSL.using(new MockConnection(context -> {
            sqls.add(context.sql());
            return new MockResult[] {
                new MockResult(0, DSL.using(SQLDialect.H2).newResult())
            };
        }), SQLDialect.H2);

        return new TraceJdbcReader(dslContext,
                                   new ObjectMapper(),
                                   schema("trace_span_summary", "custom_trace_span_summary", "startTimeUs"),
                                   schema("trace_span", CUSTOM_TRACE_SPAN_STORE, "timestamp"),
                                   schema("trace_span_tag_index", "custom_trace_span_tag_index", "timestamp"),
                                   new TraceStorageConfig(),
                                   new TestSqlDialect(),
                                   QuerySettings.DEFAULT);
    }

    private static void assertUsesCustomTraceSpanStore(String sql) {
        assertTrue(sql.contains(CUSTOM_TRACE_SPAN_STORE), sql);
        assertFalse(sql.contains("bithon_trace_span"), sql);
    }

    private static ISchema schema(String name, String store, String timestampColumn) {
        return new TestSchema(name, store, timestampColumn);
    }

    private record TestSchema(String name, String store, String timestampColumn) implements ISchema {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDisplayText() {
            return name;
        }

        @Override
        public TimestampSpec getTimestampSpec() {
            return new TimestampSpec(timestampColumn);
        }

        @Override
        public IColumn getColumnByName(String name) {
            return getColumns().stream()
                               .filter(column -> column.getName().equals(name))
                               .findFirst()
                               .orElse(null);
        }

        @Override
        public Collection<IColumn> getColumns() {
            return List.of(new StringColumn("traceId", "traceId"),
                           new StringColumn("appName", "appName"),
                           new StringColumn("instanceName", "instanceName"),
                           new StringColumn("name", "name"),
                           new StringColumn("kind", "kind"),
                           new LongColumn("timestamp", "timestamp"),
                           new LongColumn("startTimeUs", "startTimeUs"),
                           new ObjectColumn("attributes", "tags"));
        }

        @Override
        public JsonNode getInputSourceSpec() {
            return null;
        }

        @Override
        public TestDataStoreSpec getDataStoreSpec() {
            return new TestDataStoreSpec(store);
        }

        @Override
        public ISchema withDataStore(org.bithon.server.datasource.store.IDataStoreSpec spec) {
            return this;
        }

        @Override
        public void setSignature(String signature) {
        }

        @Override
        public String getSignature() {
            return null;
        }

        @Override
        public Period getTtl() {
            return null;
        }
    }

    private record TestDataStoreSpec(String store) implements org.bithon.server.datasource.store.IDataStoreSpec {
        @Override
        public String getStore() {
            return store;
        }

        @Override
        public void setSchema(ISchema schema) {
        }

        @Override
        public boolean isInternal() {
            return true;
        }

        @Override
        public IDataSourceReader createReader() {
            throw new UnsupportedOperationException();
        }
    }

    private static class TestSqlDialect implements ISqlDialect {
        @Override
        public String quoteIdentifier(String identifier) {
            return "\"" + identifier + "\"";
        }

        @Override
        public String toUnixTimestamp(IExpression timestampExpression, long intervalSeconds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IExpression timeFloor(IExpression timestampExpression, long intervalSeconds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAliasAllowedInWhereClause() {
            return true;
        }

        @Override
        public boolean needTableAlias() {
            return false;
        }

        @Override
        public IExpression toISO8601TimestampExpression(TimeSpan timeSpan) {
            return new LiteralExpression.TimestampLiteral(timeSpan.getMilliseconds());
        }

        @Override
        public String stringAggregator(String field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WindowFunctionExpression firstWindowFunction(String field, long window) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String formatDateTime(LiteralExpression.TimestampLiteral expression) {
            return expression.getValue().toString();
        }

        @Override
        public char getEscapeCharacter4SingleQuote() {
            return '\\';
        }
    }
}
