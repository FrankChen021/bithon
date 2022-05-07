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

package org.bithon.server.alerting.processor.evaluation;

import lombok.Builder;
import lombok.Data;
import org.bithon.server.alerting.common.model.AlertCondition;
import org.bithon.server.alerting.common.notification.message.ConditionEvaluationResult;
import org.bithon.server.alerting.common.notification.message.RuleMessage;

import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 23/3/22 11:40 AM
 */
@Data
@Builder
public class AlertRecordPayload {
    private long start;
    private long end;
    private int detectionLength;
    private List<AlertCondition> conditions;
    private List<RuleMessage> rules;
    private Map<String, ConditionEvaluationResult> conditionEvaluation;
}
