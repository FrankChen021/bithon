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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bithon.server.storage.alerting.Label;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 24/4/24 8:28 pm
 */
@Data
public class AlertStateObject {

    @Data
    public static class StatePerLabel {
        private AlertStatus status;

        private long matchCount;
        private long matchExpiredAt;

        private long silenceExpiredAt;
    }

    public static class Payload {
        /**
         * Status by labels
         */
        @Getter
        private final Map<Label, StatePerLabel> states;

        @Getter
        @Setter
        private long evaluationTimestamp;

        public Payload() {
            this(new HashMap<>(), 0L);
        }

        @JsonCreator
        public Payload(@JsonProperty("states") Map<Label, StatePerLabel> states,
                       @JsonProperty("evaluationTimestamp") long evaluationTimestamp) {
            this.states = states;
            this.evaluationTimestamp = evaluationTimestamp;
        }
    }

    private AlertStatus status;
    private LocalDateTime lastAlertAt;
    private String lastRecordId;
    private Payload payload;

    public AlertStatus getStatusByLabel(Label label) {
        if (payload == null) {
            return AlertStatus.READY;
        }

        StatePerLabel statusPerLabel = payload.states.get(label);
        return statusPerLabel == null ? AlertStatus.READY : statusPerLabel.status;
    }
}
