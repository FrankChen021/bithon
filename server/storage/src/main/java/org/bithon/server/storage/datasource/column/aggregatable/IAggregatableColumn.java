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

package org.bithon.server.storage.datasource.column.aggregatable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.Selector;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 5:36 下午
 */
public interface IAggregatableColumn extends IColumn {
    @JsonIgnore
    default Selector toSelector() {
        return new Selector(new Expression(getAggregateFunctionExpression()), getName());
    }

    @JsonIgnore
    default FunctionExpression getAggregateFunctionExpression() {
        throw new UnsupportedOperationException(StringUtils.format("getAggregateExpression is not supported on type of " + this.getClass().getSimpleName()));
    }
}
