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

package org.bithon.server.pipeline.common.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * @author Frank Chen
 * @date 22/1/24 10:31 pm
 */
@Slf4j
public class DropTransformer extends AbstractTransformer {
    @Getter
    private final String expr;

    @JsonIgnore
    private final IExpression dropExpression;

    // Runtime property
    private long droppedCount = 0;
    private volatile long lastLogTime = System.currentTimeMillis();

    @JsonCreator
    public DropTransformer(@JsonProperty("expr") String expr,
                           @JsonProperty("where") String where) {
        super(where);

        this.expr = Preconditions.checkArgumentNotNull("expr", expr);

        this.dropExpression = ExpressionASTBuilder.builder()
                                                  .functions(Functions.getInstance()).build(this.expr);
        AbstractTransformer.validateConditionExpression(this.dropExpression);
    }

    @Override
    protected TransformResult transformInternal(IInputRow inputRow) throws TransformException {
        boolean dropped = ((boolean) dropExpression.evaluate(inputRow));

        if (dropped) {
            long now = System.currentTimeMillis();

            synchronized (this) {
                droppedCount++;

                if (now - lastLogTime > 5_000) {
                    log.info("Expression [{}] drops [{}]", this.expr, droppedCount);

                    lastLogTime = now;
                    droppedCount = 0;
                }
            }
        }

        // When the expression satisfies, this input should be DROPPED
        return dropped ? TransformResult.DROP : TransformResult.CONTINUE;
    }
}
