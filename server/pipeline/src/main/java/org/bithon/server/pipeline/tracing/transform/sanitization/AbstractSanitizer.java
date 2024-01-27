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

package org.bithon.server.pipeline.tracing.transform.sanitization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;

/**
 * clean up some (sensitive) data in spans
 *
 * @author frank.chen021@outlook.com
 * @date 10/1/22 2:28 PM
 */
public abstract class AbstractSanitizer implements ITransformer {
    @JsonIgnore
    private IExpression whereExpression;

    protected final String where;

    public AbstractSanitizer(String where) {
        this.where = where;
        this.whereExpression = StringUtils.hasText(where) ? ExpressionASTBuilder.builder().build(where) : null;
        Preconditions.checkNotNull(this.whereExpression instanceof LogicalExpression || this.whereExpression instanceof ConditionalExpression,
                                   "The where property must be type of logical/conditional expression");
    }

    @Override
    public final boolean transform(IInputRow inputRow) throws TransformException {
        if (whereExpression != null) {
            if (!(boolean) whereExpression.evaluate(inputRow)) {
                return true;
            }
        }

        sanitize(inputRow);
        return true;
    }

    protected abstract void sanitize(IInputRow inputRow);
}
