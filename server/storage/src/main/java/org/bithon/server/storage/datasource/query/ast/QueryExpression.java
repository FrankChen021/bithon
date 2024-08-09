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

import lombok.Data;
import lombok.Getter;

/**
 * Take SQL as an example, this AST node represents a whole SELECT statement.
 * Since statement is a concept in SQL, here we don't use that concept but use 'expression',
 * so this class is called as 'SelectExpression'
 *
 *
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 14:55
 */
@Data
public class QueryExpression implements IASTNode {
    @Getter
    private final SelectorList selectorList = new SelectorList();

    private final From from = new From();
    private Where where;
    private GroupBy groupBy = new GroupBy();
    private OrderBy orderBy;
    private Limit limit;
    private Having having;

    @Override
    public void accept(IASTNodeVisitor visitor) {
        visitor.before(this);
        {
            selectorList.accept(visitor);
            from.accept(visitor);
            if (where != null) {
                where.accept(visitor);
            }
            if (groupBy != null) {
                groupBy.accept(visitor);
            }
            if (having != null) {
                having.accept(visitor);
            }
            if (orderBy != null) {
                orderBy.accept(visitor);
            }
            if (limit != null) {
                limit.accept(visitor);
            }
        }
        visitor.after(this);
    }
}
