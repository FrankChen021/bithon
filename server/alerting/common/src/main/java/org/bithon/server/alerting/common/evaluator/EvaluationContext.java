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
import org.bithon.server.alerting.common.evaluator.state.IEvaluationStateManager;
import org.bithon.server.alerting.common.evaluator.state.LocalStateManager;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frankchen
 * @date 2020-08-24 18:02:37
 */
@Getter
public class EvaluationContext implements IEvaluationContext {

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
     * The status of each (group-by) series.
     * For complex logical expression like, A by (appName) AND B by (appName), the value of the key holds the merged outputs from both A and B sub expressions
     */
    private final Map<Label, EvaluationOutputs> outputs = new HashMap<>();

    private final EvaluationLogger evaluationLogger;
    private final IDataSourceApi dataSourceApi;
    private final IEvaluationStateManager stateManager;

    public EvaluationContext(TimeSpan intervalEnd,
                             IEvaluationLogWriter logger,
                             AlertRule alertRule,
                             IDataSourceApi dataSourceApi,
                             AlertState prevState) {
        this.intervalEnd = intervalEnd;
        this.dataSourceApi = dataSourceApi;
        this.evaluationLogger = new EvaluationLogger(logger);
        this.alertRule = alertRule;
        this.stateManager = new LocalStateManager(prevState);
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
