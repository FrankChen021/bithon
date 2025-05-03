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

package org.bithon.server.storage.jdbc.metric;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.input.IInputRow;
import org.bithon.server.storage.jdbc.common.IOnceTableWriter;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
public class MetricJdbcWriter implements IMetricWriter {
    protected final DSLContext dslContext;
    protected final MetricTable table;
    protected final String insertStatement;
    private final boolean truncateDimension;
    private final Predicate<Exception> isRetryableException;

    public MetricJdbcWriter(DSLContext dslContext, MetricTable table, boolean truncateDimension, Predicate<Exception> isRetryableException) {
        this.dslContext = dslContext;
        this.table = table;
        this.truncateDimension = truncateDimension;

        int fieldCount = 1 + table.getDimensions().size() + table.getMetrics().size();
        insertStatement = dslContext.render(dslContext.insertInto(table).values(new Object[fieldCount]));

        this.isRetryableException = isRetryableException;
    }

    @Override
    public final void write(List<IInputRow> inputRowList) {
        if (CollectionUtils.isEmpty(inputRowList)) {
            return;
        }

        try {
            doInsert(createTableWriter(insertStatement, table, inputRowList));
        } catch (Throwable e) {
            log.error(StringUtils.format("Exception to insert to table [%s]:%s", table.getName(), e.getMessage()), e);
        }
    }

    protected void doInsert(IOnceTableWriter writer) throws Throwable {
        try {
            dslContext.connection(writer);
        } catch (DataAccessException e) {
            // Re-throw the caused exception for more clear stack trace
            // In such a case, the caused exception is not NULL.
            throw e.getCause();
        }
    }

    private IOnceTableWriter createTableWriter(String insertStatement, MetricTable table, List<IInputRow> inputRowList) {
        return new MetricTableWriter(insertStatement, table, inputRowList, truncateDimension, isRetryableException);
    }

    @Override
    public void close() {
    }
}
