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

package org.bithon.server.web.service.datasource.api.impl;

import lombok.Data;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpressions;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/4 00:18
 */
@Data
public class QueryAggregator {
    private String type;
    private String name;
    private String field;

    public ResultColumn toResultColumnExpression() {
        return new ResultColumn(SimpleAggregateExpressions.create(type, field == null ? name : field), name);
    }
}
