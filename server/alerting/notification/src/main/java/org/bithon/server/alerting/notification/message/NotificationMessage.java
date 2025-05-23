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

package org.bithon.server.alerting.notification.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 19/3/22 7:59 PM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String alertRecordId;
    private Map<String, AlertExpression> expressions;
    private Map<String, EvaluationOutputs> evaluationOutputs;
    private AlertRule alertRule;
    private Long lastAlertAt;
    private AlertStatus status;

    /**
     * Exclusive
     */
    private long endTimestamp;

    /**
     * Images in Base64 encoding
     * key: the alert expression id
     * val: the Base64 encoded image
     */
    private Map<String, String> images;
}
