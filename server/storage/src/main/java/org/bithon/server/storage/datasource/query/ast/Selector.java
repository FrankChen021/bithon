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

package org.bithon.server.storage.datasource.query.ast;

import lombok.Getter;
import org.bithon.component.commons.utils.StringUtils;

/**
 * Take SQL as example, this AST nodes represent a column that appears right after SELECT keyword
 * <p>
 *     e.g.
 * SELECT
 *     column1, ---------------> SelectColumn(column1)
 *     column2 AS alias2, -----> SelectColumn(column2, alias)
 *     sum(column3), ----------> SelectColumn(AggregateFunction_SUM(column3))
 *     avg(column4) AS alias4 -> SelectColumn(AggregateFunction_AVG(column4)), alias)
 * FROM table
 *
 * MetricExpression ---> Query(object)
 *   queryField ---> SelectColumn
 *
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 16:11
 */
@Getter
public class Selector implements IASTNode {
    private final IASTNode selectExpression;
    private final Alias output;

    public Selector(String name) {
        this(new Column(name), (Alias) null);
    }

    public Selector(String name, String output) {
        this(new Column(name), output == null ? null : new Alias(output));
    }

    Selector(IASTNode selectExpression) {
        this(selectExpression, (String) null);
    }

    public Selector(IASTNode selectExpression, String output) {
        this(selectExpression, output == null ? null : new Alias(output));
    }

    public Selector(IASTNode selectExpression, Alias output) {
        this.selectExpression = selectExpression;
        this.output = output;
    }

    public Selector withOutput(String alias) {
        if (this.output != null && this.output.getName().equals(alias)) {
            return this;
        }
        return new Selector(this.selectExpression, alias);
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

    @Override
    public void accept(IASTNodeVisitor visitor) {
        selectExpression.accept(visitor);
        if (output != null) {
            output.accept(visitor);
        }
    }
}
