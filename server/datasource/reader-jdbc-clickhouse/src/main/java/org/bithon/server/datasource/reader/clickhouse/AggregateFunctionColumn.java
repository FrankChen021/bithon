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

package org.bithon.server.datasource.reader.clickhouse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.Experimental;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;

import java.util.List;

/**
 * maps to ClickHouse's AggregateFunctionColumn
 *
 * @author frank.chen021@outlook.com
 * @date 22/11/24 1:00 pm
 */
@Experimental
public class AggregateFunctionColumn implements IColumn {
    private final FunctionExpression functionExpression;
    @Getter
    private final String name;

    @Getter
    private final String alias;

    @Getter
    private final IDataType dataType;

    @Getter
    @Setter
    public String aggregator;

    @JsonCreator
    public AggregateFunctionColumn(@JsonProperty("name") String name,
                                   @JsonProperty("alias") String alias,
                                   @JsonProperty("aggregator") String aggregator,
                                   @JsonProperty("dataType") IDataType dataType) {
        this.name = name;
        this.alias = StringUtils.isEmpty(alias) ? name : alias;
        this.dataType = dataType;
        this.aggregator = aggregator;
        this.functionExpression = FunctionExpression.create("sum".equals(aggregator) ? SumMergeFunction.INSTANCE : CountMergeFunction.INSTANCE,
                                                            IdentifierExpression.of(name, dataType));
    }

    @Override
    public Selector toSelector() {
        return new Selector(new ExpressionNode(functionExpression), getName());
    }

    /**
     * Create an aggregated function call expression on this AggregateColumn
     *
     */
    @Override
    public IExpression createAggregateFunctionExpression(IFunction function) {
        if (function instanceof AggregateFunction.Sum) {
            if ("sum".equals(this.aggregator)) {
                // Still use SUM, but the ClickHouseExpressionOptimizer will replace it with sumMerge
                return new FunctionExpression(AggregateFunction.Sum.INSTANCE, IdentifierExpression.of(name, dataType));
            } else {
                throw new HttpMappableException(400, "Invalid aggregator [%s] on column [AggregateFunction(%s,%s)]", function.getName(), this.aggregator, this.dataType);
            }
        } else if (function instanceof AggregateFunction.Count) {
            if ("count".equals(this.aggregator)) {
                // Still use COUNT, but the ClickHouseExpressionOptimizer will replace it with sumMerge
                return new FunctionExpression(AggregateFunction.Count.INSTANCE, IdentifierExpression.of(name, IDataType.LONG));
            } else {
                // otherwise, treat it as a normal count operation
                return new FunctionExpression(function, IdentifierExpression.of(name, IDataType.LONG));
            }
        } else {
            return new FunctionExpression(function, IdentifierExpression.of(name, dataType));
        }
    }

    public static class SumMergeFunction extends AggregateFunction.Sum {
        public static final AggregateFunction INSTANCE = new SumMergeFunction();

        public SumMergeFunction() {
            super("sumMerge");
        }

        @Override
        public Object evaluate(List<Object> args) {
            return null;
        }
    }

    public static class CountMergeFunction extends AggregateFunction.Count {
        public static final AggregateFunction INSTANCE = new CountMergeFunction();

        public CountMergeFunction() {
            super("countMerge");
        }

        @Override
        public Object evaluate(List<Object> args) {
            return null;
        }
    }
}
