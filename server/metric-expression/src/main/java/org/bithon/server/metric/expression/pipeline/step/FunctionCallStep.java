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

package org.bithon.server.metric.expression.pipeline.step;


import org.bithon.server.datasource.query.pipeline.IQueryStep;
import org.bithon.server.datasource.query.pipeline.PipelineQueryResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Map to the {@link org.bithon.component.commons.expression.FunctionExpression}
 *
 * @author frank.chen021@outlook.com
 * @date 2/6/25 9:29 pm
 */
public abstract class FunctionCallStep implements IQueryStep {
    private final IQueryStep source;

    public FunctionCallStep(IQueryStep source) {
        this.source = source;
    }

    @Override
    public boolean isScalar() {
        return source.isScalar();
    }

    public static FunctionCallStep apply(IQueryStep source,
                                         Function<PipelineQueryResult, PipelineQueryResult> function) {
        return new FunctionCallStep(source) {
            @Override
            public CompletableFuture<PipelineQueryResult> execute() throws Exception {
                return source.execute().thenApply(function);
            }
        };
    }

    public static Function<PipelineQueryResult, PipelineQueryResult> SUM = (result) -> {
        return result;
    };
}
