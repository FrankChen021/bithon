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

package org.bithon.server.alerting.manager.api.parameter;


import lombok.Data;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 29/4/25 12:26 am
 */
@Data
public class RuleFolderVO {
    private String folder;
    private int ruleCount;

    // Aggregation on all rules under this folder
    private Long lastEvaluatedAt;
    private Long lastAlertedAt;
    private long lastUpdatedAt;

    public void updateCount() {
        this.ruleCount++;
    }

    public void updateLastEvaluatedAt(Timestamp value) {
        if (value != null) {
            if (this.lastEvaluatedAt == null) {
                this.lastEvaluatedAt = value.getTime();
            } else {
                this.lastEvaluatedAt = Math.max(value.getTime(), this.lastEvaluatedAt);
            }
        }
    }

    public void updateLastAlertedAt(Timestamp value) {
        if (value != null) {
            if (this.lastAlertedAt == null) {
                this.lastAlertedAt = value.getTime();
            } else {
                this.lastAlertedAt = Math.max(value.getTime(), this.lastAlertedAt);
            }
        }
    }

    public void updateLastUpdatedAt(Timestamp lastUpdatedAt) {
        if (lastUpdatedAt != null) {
            this.lastUpdatedAt = Math.max(lastUpdatedAt.getTime(), this.lastUpdatedAt);
        }
    }
}
