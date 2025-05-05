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

package org.bithon.server.datasource.reader.vm;


import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Query;
import org.bithon.server.datasource.query.ast.Expression;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.pipeline.Column;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 28/4/25 9:35 pm
 */
public class VMDataSourceReader implements IDataSourceReader {
    private final IVMQueryRpc queryApi;

    public VMDataSourceReader(String url, ApplicationContext applicationContext) {

        this.queryApi = Feign.builder()
                             .contract(applicationContext.getBean(Contract.class))
                             .encoder(applicationContext.getBean(Encoder.class))
                             .decoder(applicationContext.getBean(Decoder.class))
                             .target(IVMQueryRpc.class, url);

    }

    @Override
    public ColumnarTable timeseries(Query query) {
        Selector selector = query.getSelectors().get(0);
        if (!(selector.getSelectExpression() instanceof Expression expression)) {
            throw new UnsupportedOperationException("Unsupported select expression " + selector.getSelectExpression());
        }
        if (!(expression.getParsedExpression() instanceof FunctionExpression functionExpression)) {
            throw new UnsupportedOperationException("Unsupported expression " + expression.getParsedExpression().serializeToText());
        }
        if (!functionExpression.getFunction().isAggregator()) {
            throw new UnsupportedOperationException("Unsupported function " + functionExpression.getFunction());
        }
        IExpression args = functionExpression.getArgs().get(0);
        if (!(args instanceof IdentifierExpression identifierExpression)) {
            throw new UnsupportedOperationException("Unsupported metric field " + args);
        }

        String aggregator = functionExpression.getFunction().getName();
        String metric = identifierExpression.getIdentifier();
        String labelSelector = LabelSelectorSerializer.toString(query.getFilter());

        StringBuilder ql = new StringBuilder(128);
        ql.append(aggregator);
        if (CollectionUtils.isNotEmpty(query.getGroupBy())) {
            ql.append(" by (");
            for (int i = 0; i < query.getGroupBy().size(); i++) {
                if (i > 0) {
                    ql.append(',');
                }
                ql.append(query.getGroupBy().get(i));
            }
            ql.append(')');
            ql.append(' ');
        }
        ql.append('(');
        {
            ql.append(metric);
            ql.append('{');
            ql.append(labelSelector);
            ql.append('}');
        }
        ql.append(')');
        if (query.getInterval().getWindow() != null) {
            ql.append('[');
            ql.append(query.getInterval().getWindow());
            ql.append(']');
        }

        IVMQueryRpc.RangeQueryResponse response = queryApi.queryRange(ql.toString(),
                                                                      query.getInterval().getStartTime().getSeconds(),
                                                                      query.getInterval().getEndTime().getSeconds(),
                                                                      query.getInterval().getStep().getSeconds() + "s");
        if (!"success".equals(response.getStatus())) {
            throw new HttpMappableException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Failed to query VM data source: " + response.getStatus());
        }

        ColumnarTable table = new ColumnarTable();
        Column tsColumn = table.addColumn(Column.create(TimestampSpec.COLUMN_ALIAS, IDataType.LONG, 256));
        Column valColumn = table.addColumn(Column.create(metric, IDataType.DOUBLE, 256));

        for (IVMQueryRpc.SeriesData series : response.getData().getResult()) {
            for (double[] valuesInRow : series.getValues()) {
                // Return timestamp in SECONDS
                tsColumn.addLong((long) valuesInRow[0]);
                valColumn.addDouble(valuesInRow[1]);
            }
        }

        return table;
    }

    @Override
    public List<?> groupBy(Query query) {
        return List.of();
    }

    @Override
    public List<?> select(Query query) {
        return List.of();
    }

    @Override
    public int count(Query query) {
        return 0;
    }

    @Override
    public List<String> distinct(Query query) {
        return List.of();
    }
}
