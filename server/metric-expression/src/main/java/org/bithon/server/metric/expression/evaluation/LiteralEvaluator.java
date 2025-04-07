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

package org.bithon.server.metric.expression.evaluation;


import org.bithon.component.commons.expression.LiteralExpression;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 9:36 pm
 */
public class LiteralEvaluator implements IEvaluator {
    private final Object value;

    public LiteralEvaluator(LiteralExpression<?> value) {
        this.value = value.getValue();
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public CompletableFuture<EvaluationResult> evaluate() {
        return CompletableFuture.completedFuture(EvaluationResult.builder()
                                                                 .valueNames(List.of("value"))
                                                                 .values(Map.of("value", List.of(value)))
                                                                 .build());
    }
}
