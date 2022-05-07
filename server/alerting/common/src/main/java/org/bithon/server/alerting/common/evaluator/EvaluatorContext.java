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
import org.bithon.server.alerting.common.evaluator.result.EvaluationResult;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.model.AlertCompositeConditions;
import org.bithon.server.alerting.common.model.AlertCondition;
import org.bithon.server.alerting.common.model.IMetricCondition;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IEvaluatorLogWriter;
import org.bithon.server.web.service.api.IDataSourceApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frankchen
 * @date 2020-08-24 18:02:37
 */
@Getter
public class EvaluatorContext {
    private final TimeSpan intervalEnd;
    private final IEvaluatorLogWriter evaluatorLogger;
    private final Alert alert;
    private final List<AlertCompositeConditions> matchedRules = new ArrayList<>();
    private final Map<String, IEvaluationOutput> evaluatedConditions = new HashMap<>();
    private final List<IEvaluationOutput> evaluatedOutputs = new ArrayList<>();
    private final Map<String, EvaluationResult> evaluationResults = new HashMap<>();
    private final IDataSourceApi dataSourceApi;

    /**
     * current condition id that is under evaluation
     */
    @Setter
    private AlertCondition evaluatingCondition;

    @Setter
    private IMetricCondition evaluatingMetric;

    public EvaluatorContext(TimeSpan intervalEnd,
                            IEvaluatorLogWriter logger,
                            Alert alert,
                            IDataSourceApi dataSourceApi) {
        this.intervalEnd = intervalEnd;
        this.dataSourceApi = dataSourceApi;
        this.evaluatorLogger = logger;
        this.alert = alert;
        this.alert.getConditions().forEach((condition) -> evaluationResults.put(condition.getId(), EvaluationResult.UNEVALUATED));
    }

    public EvaluationResult getConditionEvaluationResult(String conditionId) {
        return evaluationResults.get(conditionId);
    }

    public IEvaluationOutput getConditionEvaluationOutput(String conditionId) {
        return evaluatedConditions.get(conditionId);
    }

    public void setConditionEvaluationResult(String conditionId,
                                             boolean matches,
                                             IEvaluationOutput output) {

        this.evaluationResults.put(conditionId, matches ? EvaluationResult.MATCHED : EvaluationResult.UNMATCHED);
        if (output != null) {
            evaluatedConditions.put(conditionId, output);
        }
    }

    public void addMatchedTrigger(AlertCompositeConditions trigger) {
        matchedRules.add(trigger);
    }

    public void log(Class<?> loggerClass, String message) {
        this.evaluatorLogger.log(this.alert.getId(), this.alert.getName(), loggerClass, message);
    }

    public void log(Class<?> loggerClass, String messageFormat, Object... args) {
        this.evaluatorLogger.log(this.alert.getId(), this.alert.getName(), loggerClass, messageFormat, args);
    }

    public void logException(Class<?> loggerClass,
                             Throwable e,
                             String format,
                             Object... args) {
        this.evaluatorLogger.error(this.alert.getId(), alert.getName(), loggerClass, e, format, args);
    }
}
