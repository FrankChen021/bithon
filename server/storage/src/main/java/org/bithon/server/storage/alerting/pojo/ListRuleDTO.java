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

package org.bithon.server.storage.alerting.pojo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6
 */
@Data
public class ListRuleDTO {
    private String id;
    private String name;
    private String appName;

    private boolean disabled;
    private boolean deleted;
    private String payload;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String lastOperator;

    // From bithon_alert_state
    private Timestamp lastEvaluatedAt;
    private Timestamp lastAlertAt;
    private String lastRecordId;
    private AlertStatus alertStatus;
}
