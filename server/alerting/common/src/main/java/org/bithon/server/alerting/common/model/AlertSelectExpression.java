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

package org.bithon.server.alerting.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/26 21:38
 */
@Getter
public class AlertSelectExpression {
    /**
     * the output name. Can be seen as the name in 'AS' expression of a SQL.
     */
    private final String name;

    private final AggregatorEnum aggregator;

    @JsonCreator
    public AlertSelectExpression(@JsonProperty("name") String name,
                                 @JsonProperty("aggregator") AggregatorEnum aggregator) {
        this.name = name;
        this.aggregator = aggregator;
    }
}
