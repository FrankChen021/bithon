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

package org.bithon.server.pipeline.common.transform.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * @author frank.chen021@outlook.com
 * @date 4/8/22 4:38 PM
 */
public class ExpressionFilter implements IInputRowFilter {

    @Getter
    private final String expression;

    @Getter
    private final boolean debug;

    private final IExpression delegation;

    @VisibleForTesting
    public ExpressionFilter(String expression) {
        this(expression, false);
    }

    @JsonCreator
    public ExpressionFilter(@JsonProperty("expression") String expression,
                            @JsonProperty("debug") Boolean debug) {
        this.expression = expression;
        this.debug = debug != null && debug;
        this.delegation = ExpressionASTBuilder.builder().functions(Functions.getInstance()).build(this.expression);
    }

    @Override
    public boolean shouldInclude(IInputRow inputRow) {
        return (boolean) delegation.evaluate(inputRow);
    }
}
