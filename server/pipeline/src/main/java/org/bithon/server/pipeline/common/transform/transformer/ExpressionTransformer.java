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

package org.bithon.server.pipeline.common.transform.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * @author Frank Chen
 * @date 12/3/24 6:36 pm
 */
public class ExpressionTransformer extends AbstractTransformer {

    @Getter
    private final String expr;

    @Getter
    private final String target;


    // Runtime property
    @JsonIgnore
    private final IExpression astExpression;

    @JsonCreator
    public ExpressionTransformer(@JsonProperty("expr") String expr,
                                 @JsonProperty("target") String target,
                                 @JsonProperty("where") String where) {
        super(where);

        this.expr = Preconditions.checkNotNull(expr, "'expr' property can not be NULL");
        this.target = Preconditions.checkNotNull(target, "'target' property can not be NULL");

        this.astExpression = ExpressionASTBuilder.builder().functions(Functions.getInstance()).build(this.expr);
    }

    @Override
    protected TransformResult transformInternal(IInputRow inputRow) throws TransformException {
        Object ret = astExpression.evaluate(inputRow);
        if (ret != null) {
            inputRow.updateColumn(this.target, ret);
        }

        return TransformResult.CONTINUE;
    }
}
