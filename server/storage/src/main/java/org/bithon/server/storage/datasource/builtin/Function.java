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

package org.bithon.server.storage.datasource.builtin;

import lombok.Getter;
import org.bithon.server.datasource.aggregator.ast.PostAggregatorExpressionParser;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/2 17:31
 */
@Getter
public class Function {
    private final String name;
    private final List<Parameter> parameters;

    public Function(String name, BiConsumer<Integer, PostAggregatorExpressionParser.ExpressionContext> validator) {
        this(name, Collections.emptyList(), validator);
    }

    public Function(String name, List<Parameter> parameters, BiConsumer<Integer, PostAggregatorExpressionParser.ExpressionContext> validator) {
        this.name = name;
        this.parameters = parameters;
        this.validator = validator;
    }

    private final BiConsumer<Integer, PostAggregatorExpressionParser.ExpressionContext> validator;
}
