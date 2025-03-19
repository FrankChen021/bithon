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

import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.evaluator.result.EvaluationStatus;
import org.bithon.server.alerting.common.evaluator.result.ExpressionEvaluationResult;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frankchen
 * @date 2020-08-24 18:02:37
 */
@Getter
public class EvaluationContext implements IEvaluationContext {

    private final @Nullable AlertState prevState;

    private final TimeSpan intervalEnd;
    private final AlertRule alertRule;

    /**
     * current expression that is under evaluation
     */
    @Setter
    private AlertExpression evaluatingExpression;

    @Setter
    private boolean isExpressionEvaluatedAsTrue = false;
    /**
     * Result per each {@link AlertExpression} for each label
     */
    private final Map<String, ExpressionEvaluationResult> evaluationResult = new HashMap<>();

    /**
     * The outputs of whole alert rule.
     * For simple expression, it's the SAME as above.
     * For complex expression like A AND B, it's the intersection result set of A and B
     */
    @Setter
    private EvaluationOutputs outputs;

    /**
     * The status of each (group-by) series
     */
    private final Map<Label, AlertStatus> seriesStatus = new HashMap<>();

    private final EvaluationLogger evaluationLogger;
    private final IDataSourceApi dataSourceApi;

    public EvaluationContext(TimeSpan intervalEnd,
                             IEvaluationLogWriter logger,
                             AlertRule alertRule,
                             IDataSourceApi dataSourceApi,
                             AlertState prevState) {
        this.intervalEnd = intervalEnd;
        this.dataSourceApi = dataSourceApi;
        this.evaluationLogger = new EvaluationLogger(logger);
        this.alertRule = alertRule;
        this.prevState = prevState;
    }

    public void setEvaluationResult(String expressionId,
                                    boolean matches,
                                    EvaluationOutputs outputs) {

        ExpressionEvaluationResult result = this.evaluationResult.computeIfAbsent(expressionId, (k) -> new ExpressionEvaluationResult(EvaluationStatus.UNEVALUATED, new EvaluationOutputs()));
        result.setResult(matches ? EvaluationStatus.MATCHED : EvaluationStatus.UNMATCHED);
        if (outputs != null) {
            result.setOutputs(outputs);
        }
    }

    public void log(Class<?> loggerClass, String message) {
        this.evaluationLogger.info(this.alertRule.getId(), this.alertRule.getName(), loggerClass, message);
    }

    public void log(Class<?> loggerClass, String messageFormat, Object... args) {
        this.evaluationLogger.info(this.alertRule.getId(), this.alertRule.getName(), loggerClass, messageFormat, args);
    }

    public void logException(Class<?> loggerClass,
                             Throwable e,
                             String format,
                             Object... args) {
        this.evaluationLogger.error(this.alertRule.getId(), alertRule.getName(), loggerClass, e, format, args);
    }

    @Override
    public Object get(String name) {
        return null;
    }
}
