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

package org.bithon.server.alerting.common.evaluator;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.web.service.api.IDataSourceApi;

import java.util.List;

/**
 * @author frankchen
 * @date 2020-08-25 14:01:34
 */
public class EvaluatorDecoratorWithLogger implements IConditionEvaluator {

    private final IConditionEvaluator raw;

    public EvaluatorDecoratorWithLogger(IConditionEvaluator raw) {
        this.raw = raw;
    }

    @Override
    public IEvaluationOutput evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      TimeSpan start,
                                      TimeSpan end,
                                      List<IFilter> dimensions,
                                      EvaluatorContext context) {
        try {
            IEvaluationOutput output = raw.evaluate(dataSourceApi,
                                                    dataSource,
                                                    start,
                                                    end,
                                                    dimensions,
                                                    context);

            String text = StringUtils.format("Evaluated condition [%s] %s. Current window: [%s], Incremental: [%s], Result: [%s]",
                                             context.getEvaluatingCondition().getId(),
                                             context.getEvaluatingMetric().toString(),
                                             output == null ? "null" : output.getCurrentText(),
                                             output == null ? "null" : output.getDeltaText(),
                                             (output != null && output.isMatches()) ? "Matched" : "NOT Matched");
            context.log(EvaluatorDecoratorWithLogger.class, text);

            return output;
        } catch (Exception e) {
            context.logException(EvaluatorDecoratorWithLogger.class,
                                 e,
                                 "Condition [%s]: %s exception during evaluation. %s",
                                 context.getEvaluatingCondition().getId(),
                                 context.getEvaluatingMetric().toString(),
                                 e);
            return null;
        }
    }
}
