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

package org.bithon.server.storage.jdbc.metric;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.api.CardinalityAggregator;
import org.bithon.server.storage.datasource.api.IQuerableAggregatorVisitor;

/**
 * @author Frank Chen
 * @date 1/11/21 3:11 pm
 */
public class QuerableAggregatorSqlVisitor implements IQuerableAggregatorVisitor<String> {

    @Override
    public String visit(CardinalityAggregator aggregator) {
        return StringUtils.format("count(DISTINCT \"%s\") AS \"%s\"", aggregator.getDimension(), aggregator.getName());
    }
}
