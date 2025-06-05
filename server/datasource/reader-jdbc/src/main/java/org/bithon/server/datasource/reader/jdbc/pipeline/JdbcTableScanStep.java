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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.result.Column;
import org.bithon.server.datasource.query.result.ColumnarTable;
import org.bithon.server.datasource.query.result.PipelineQueryResult;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TableIdentifier;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/6/5 19:58
 */
@Slf4j
public class JdbcTableScanStep implements IPhysicalPlan {
    private final DSLContext dslContext;
    private final boolean isScalar;
    private final ISqlDialect sqlDialect;

    @Getter
    private SelectStatement selectStatement;

    public JdbcTableScanStep(DSLContext dslContext,
                             ISqlDialect sqlDialect,
                             boolean isScalar,
                             SelectStatement selectStatement) {
        this.dslContext = dslContext;
        this.sqlDialect = sqlDialect;
        this.isScalar = isScalar;
        this.selectStatement = selectStatement;
    }

    @Override
    public boolean isScalar() {
        return this.isScalar;
    }

    @Override
    public void serializer(StringBuilder builder) {
        builder.append("TableScan\n");
        builder.append(selectStatement.toSQL(this.sqlDialect));
        builder.append("\n");
    }

    @Override
    public CompletableFuture<PipelineQueryResult> execute() throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            ColumnarTable resultTable = new ColumnarTable();
            for (Selector selector : selectStatement.getSelectorList().getSelectors()) {
                resultTable.addColumn(Column.create(selector.getOutputName(), selector.getDataType(), 1024));
            }
            List<Column> resultColumns = resultTable.getColumns();

            String sql = selectStatement.toSQL(this.sqlDialect);

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
