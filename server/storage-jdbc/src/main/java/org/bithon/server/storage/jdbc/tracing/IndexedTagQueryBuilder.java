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

package org.bithon.server.storage.jdbc.tracing;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.jdbc.common.dialect.Expression2Sql;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/7 15:42
 */
class IndexedTagQueryBuilder extends NestQueryBuilder {

    private final DataSourceSchema traceTagIndexSchema;

    IndexedTagQueryBuilder(DataSourceSchema traceTagIndexSchema) {
        this.traceTagIndexSchema = traceTagIndexSchema;
    }

    @Override
    public SelectConditionStep<Record1<String>> build(Map<Integer, IExpression> filters) {
        if (filters.isEmpty()) {
            return null;
        }

        SelectConditionStep<Record1<String>> query = null;

        for (Map.Entry<Integer, IExpression> entry : filters.entrySet()) {
            Integer index = entry.getKey();
            IExpression filter = entry.getValue();

            if (index > Tables.BITHON_TRACE_SPAN_TAG_INDEX.fieldsRow().size() - 2) {
                throw new RuntimeException(StringUtils.format("Tag [%s] is configured to use wrong index [%d]. Should be in the range [1, %d]",
                                                              filter.serializeToText(),
                                                              index,
                                                              Tables.BITHON_TRACE_SPAN_TAG_INDEX.fieldsRow().size() - 2));
            }

            if (query == null) {
                query = dslContext.select(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TRACEID)
                                  .from(Tables.BITHON_TRACE_SPAN_TAG_INDEX)
                                  .where(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.ge(this.start))
                                  .and(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.lt(this.end));
            }
            query = query.and(new TagFilterSerializer(index).serialize(filter));
        }

        if (query != null) {
            if (this.in != null) {
                return query.and(Tables.BITHON_TRACE_SPAN.TRACEID.in(this.in));
            } else {
                return query;
            }
        } else {
            return this.in;
        }
    }

    static class TagFilterSerializer extends Expression2Sql {
        private final int index;

        TagFilterSerializer(int index) {
            super(null, true);
            this.index = index;
        }

        @Override
        public boolean visit(IdentifierExpression expression) {
            sb.append('"');
            sb.append('f');
            sb.append(index);
            sb.append('"');
            return false;
        }
    }
}
