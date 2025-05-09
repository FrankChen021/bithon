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

package org.bithon.server.datasource.reader.jdbc.statement.ast;

import lombok.Getter;
import org.bithon.server.datasource.query.Order;
import org.bithon.server.datasource.query.ast.IASTNode;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 14:55
 */
public class OrderByClause implements IASTNode {

    @Getter
    private final String field;

    @Getter
    private final Order order;

    /**
     * @param field
     * @param order ASC or DESC
     */
    public OrderByClause(String field, Order order) {
        this.field = field;
        this.order = order;
    }
}
