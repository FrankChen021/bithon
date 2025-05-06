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

import org.bithon.component.commons.expression.IExpression;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.Expression2Sql;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/7 15:42
 */
class IndexedTagQueryBuilder extends NestQueryBuilder {

    private final ISqlDialect sqlDialect;

    IndexedTagQueryBuilder(ISqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public SelectConditionStep<Record1<String>> build(List<IExpression> filters) {
        if (filters.isEmpty()) {
            return null;
        }

        SelectConditionStep<Record1<String>> query = null;
        for (IExpression filter : filters) {
            if (query == null) {
                query = dslContext.select(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TRACEID)
                                  .from(Tables.BITHON_TRACE_SPAN_TAG_INDEX)
                                  .where(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.ge(this.start))
                                  .and(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.lt(this.end));
            }

            // NOTE:
            // instantiate the TagFilterSerializer for each loop
            // because it internally holds some states for each 'serialize' method call
            query = query.and(new Expression2Sql(null, sqlDialect).serialize(filter));
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
}
