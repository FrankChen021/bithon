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

package org.bithon.server.datasource.query.plan.logical;

import jakarta.annotation.Nullable;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.utils.HumanReadableDuration;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/6/4 23:33
 */
public record LogicalScalar(
    LiteralExpression<?> literal,
    @Nullable HumanReadableDuration offset
) implements ILogicalPlan {

    public LogicalScalar(LiteralExpression<?> literal) {
        this(literal, null);
    }

    @Override
    public <T> T accept(ILogicalPlanVisitor<T> visitor) {
        return visitor.visitScalar(this);
    }
}
