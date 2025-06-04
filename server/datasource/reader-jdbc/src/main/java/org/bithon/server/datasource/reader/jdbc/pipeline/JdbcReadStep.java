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

package org.bithon.server.datasource.reader.jdbc.pipeline;


import lombok.extern.slf4j.Slf4j;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.physical.Column;
import org.bithon.server.datasource.query.plan.physical.ColumnarTable;
import org.bithon.server.datasource.query.plan.physical.IQueryStep;
import org.bithon.server.datasource.query.plan.physical.PipelineQueryResult;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 5/5/25 6:14 pm
 */
@Slf4j
public class JdbcReadStep implements IQueryStep {

    private final DSLContext dslContext;
    private final SelectStatement selectStatement;
    private final String sql;
    private final boolean isScalar;

    public JdbcReadStep(DSLContext dslContext,
                        ISqlDialect sqlDialect,
                        SelectStatement selectStatement,
                        boolean isScalar) {
        this.dslContext = dslContext;
        this.selectStatement = selectStatement;
        this.isScalar = isScalar;

        this.sql = selectStatement.toSQL(sqlDialect);
    }

    @Override
    public String toString() {
        return this.sql;
    }

    @Override
    public boolean isScalar() {
        return this.isScalar;
    }

    @Override
    public CompletableFuture<PipelineQueryResult> execute() throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            ColumnarTable resultTable = new ColumnarTable();
            for (Selector selector : selectStatement.getSelectorList().getSelectors()) {
                resultTable.addColumn(Column.create(selector.getOutputName(), selector.getDataType(), 1024));
            }
            List<Column> resultColumns = resultTable.getColumns();

            log.info("Executing {}", sql);
            try (Cursor<org.jooq.Record> cursor = dslContext.fetchLazy(sql)) {
                for (Record record : cursor) {
                    for (int i = 0; i < resultColumns.size(); i++) {
                        Column column = resultColumns.get(i);
                        column.addObject(record.get(i));
                    }
                }
                return PipelineQueryResult.builder()
                                          .table(resultTable)
                                          .build();
            }
        });
    }
}
