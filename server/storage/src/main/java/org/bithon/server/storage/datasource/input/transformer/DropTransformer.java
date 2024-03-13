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

package org.bithon.server.storage.datasource.input.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * @author Frank Chen
 * @date 22/1/24 10:31 pm
 */
public class DropTransformer implements ITransformer {
    @Getter
    private final String expression;

    @Getter
    private final boolean debug;

    private final IExpression astExpression;

    @VisibleForTesting
    public DropTransformer(String expression) {
        this(expression, false);
    }

    @JsonCreator
    public DropTransformer(@JsonProperty("expr") String expression,
                           @JsonProperty("debug") Boolean debug) {
        this.expression = expression;
        this.debug = debug != null && debug;
        this.astExpression = ExpressionASTBuilder.builder().functions(Functions.getInstance()).build(this.expression);
        Preconditions.checkIfTrue(this.astExpression instanceof LogicalExpression || this.astExpression instanceof ConditionalExpression,
                                  "The expr property must be a LOGICAL or CONDITIONAL expression");
    }

    @Override
    public boolean transform(IInputRow inputRow) throws TransformException {
        // When the expression satisfies, this input should be DROPPED
        return !((boolean) astExpression.evaluate(inputRow));
    }
}
