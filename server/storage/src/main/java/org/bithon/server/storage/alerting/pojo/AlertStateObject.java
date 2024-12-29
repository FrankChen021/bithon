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
import org.bithon.server.storage.alerting.Labels;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 24/4/24 8:28 pm
 */
@Data
public class AlertStateObject {

    @Data
    public static class Payload {
        /**
         * Status by labels
         */
        private Map<Labels, AlertStatus> status;
    }

    private AlertStatus status;
    private LocalDateTime lastAlertAt;
    private String lastRecordId;
    private Payload payload;

    public AlertStatus getStatusByLabel(Labels labels) {
        if (payload == null) {
            return AlertStatus.READY;
        }

        return payload.status.getOrDefault(labels, AlertStatus.READY);
    }
}
