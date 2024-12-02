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

package org.bithon.server.alerting.evaluator.evaluator;

import lombok.Builder;
import lombok.Data;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.notification.message.ExpressionEvaluationResult;

import java.util.Collection;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 23/3/22 11:40 AM
 */
@Data
@Builder
public class AlertRecordPayload {
    // Evaluation start, inclusive
    private long start;

    // Evaluation end, inclusive
    private long end;

    private Collection<AlertExpression> expressions;
    private Map<String, ExpressionEvaluationResult> conditionEvaluation;
}
