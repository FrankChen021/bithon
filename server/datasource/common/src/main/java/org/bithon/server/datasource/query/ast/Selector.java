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

package org.bithon.server.datasource.query.ast;

import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.utils.StringUtils;

/**
 * Take SQL as example, this AST nodes represent a column that appears right after SELECT keyword.
 * For example:
 * <pre>
 * SELECT
 *     column1, ---------------> Selector(Column(column1))
 *     column2 AS alias2, -----> Selector(Column(column2), alias)
 *     sum(column3), ----------> Selector(Expression(AggregateFunction_SUM(column3)))
 *     avg(column4) AS alias4 -> Selector(Expression(AggregateFunction_SUM(column3)), alias4)
 * FROM table
 * </pre>
 *
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 16:11
 */
@Getter
public class Selector implements IASTNode {
    private final IASTNode selectExpression;

    @Setter
    private Alias output;

    @Setter
    private Object tag;

    private final IDataType dataType;

    public Selector(String name, IDataType dataType) {
        this(new Column(name), (Alias) null, dataType);
    }

    public Selector(String name, String output, IDataType dataType) {
        this(new Column(name), output == null ? null : new Alias(output), dataType);
    }

    public Selector(IASTNode selectExpression, String output, IDataType dataType) {
        this(selectExpression, output == null ? null : new Alias(output), dataType);
    }

    public Selector(ExpressionNode selectExpression, String output) {
        this(selectExpression, output, selectExpression.getDataType());
    }

    public Selector(ExpressionNode selectExpression, Alias output) {
        this(selectExpression, output, selectExpression.getDataType());
    }

    public Selector(IASTNode selectExpression, Alias output, IDataType dataType) {
        this.selectExpression = selectExpression;
        this.output = output;
        this.dataType = dataType;
    }

    public Selector withOutput(String alias) {
        if (this.output != null && this.output.getName().equals(alias)) {
            return this;
        }
        return new Selector(this.selectExpression, alias, this.dataType);
    }

    public String getOutputName() {
        if (output != null) {
            return output.getName();
        }
        if (selectExpression instanceof Column) {
            return ((Column) selectExpression).getName();
        }
        throw new RuntimeException(StringUtils.format("no result name for result column [%s]", selectExpression));
    }
}
