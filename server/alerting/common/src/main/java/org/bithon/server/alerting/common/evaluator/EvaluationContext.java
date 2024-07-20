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
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.result.EvaluationResult;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author frankchen
 * @date 2020-08-24 18:02:37
 */
@Getter
public class EvaluationContext implements IEvaluationContext {
    private final TimeSpan intervalEnd;
    private final EvaluationLogger evaluationLogger;
    private final AlertRule alertRule;
    private final Map<String, IEvaluationOutput> evaluatedExpressions = new HashMap<>();

    // Use LinkedHashMap to keep the order of expressions
    private final Map<String, AlertExpression> alertExpressions = new LinkedHashMap<>();
    private final Map<String, EvaluationResult> evaluationResults = new HashMap<>();
    private final IDataSourceApi dataSourceApi;
    private final @Nullable AlertStateObject prevState;

    /**
     * current condition id that is under evaluation
     */
    @Setter
    private AlertExpression evaluatingExpression;

    public EvaluationContext(TimeSpan intervalEnd,
                             IEvaluationLogWriter logger,
                             AlertRule alertRule,
                             IDataSourceApi dataSourceApi,
                             AlertStateObject prevState) {
        this.intervalEnd = intervalEnd;
        this.dataSourceApi = dataSourceApi;
        this.evaluationLogger = new EvaluationLogger(logger);
        this.alertRule = alertRule;
        this.prevState = prevState;

        this.alertRule.getFlattenExpressions().forEach((id, alertExpression) -> {
            evaluationResults.put(id, EvaluationResult.UNEVALUATED);
        });
        this.alertExpressions.putAll(alertRule.getFlattenExpressions());
    }

    public IEvaluationOutput getRuleEvaluationOutput(String ruleId) {
        return evaluatedExpressions.get(ruleId);
    }

    public void setEvaluationResult(String ruleId,
                                    boolean matches,
                                    IEvaluationOutput output) {

        this.evaluationResults.put(ruleId, matches ? EvaluationResult.MATCHED : EvaluationResult.UNMATCHED);
        if (output != null) {
            evaluatedExpressions.put(ruleId, output);
        }
    }

    public void log(Class<?> loggerClass, String message) {
        this.evaluationLogger.log(this.alertRule.getId(), this.alertRule.getName(), loggerClass, message);
    }

    public void log(Class<?> loggerClass, String messageFormat, Object... args) {
        this.evaluationLogger.log(this.alertRule.getId(), this.alertRule.getName(), loggerClass, messageFormat, args);
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
