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

package org.bithon.server.datasource.vm;


import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 28/4/25 10:09 pm
 */
public class LabelSelectorSerializer extends ExpressionSerializer {
    public LabelSelectorSerializer(IdentifierQuotaStrategy quoteIdentifier) {
        super(quoteIdentifier);
    }

    @Override
    public void serialize(LogicalExpression expression) {
        List<IExpression> operands = expression.getOperands();
        for (int i = 0, operandsSize = operands.size(); i < operandsSize; i++) {
            IExpression expr = operands.get(i);
            if (!(expr instanceof ComparisonExpression)) {
                throw new UnsupportedOperationException("Unsupported expression type: " + expr.serializeToText());
            }

            if (i > 0) {
                sb.append(',');
            }
            expr.serializeToText(this);
        }
    }

    public static String toString(IExpression expression) {
        if (expression == null) {
            return "";
        }

        LabelSelectorSerializer serializer = new LabelSelectorSerializer(IdentifierQuotaStrategy.NONE);
        expression.serializeToText(serializer);
        return serializer.getSerializedText();
    }
}
