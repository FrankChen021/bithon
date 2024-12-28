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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.component.commons.utils.HumanReadableDuration;

import java.util.List;

/**
 * NOTE: The serialization order is manually controlled so that people understand an alert in a natural way
 *
 * @author frank.chen021@outlook.com
 * @date 2024/2/11 19:51
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"expr", "for", "every", "silence", "notifications", "notificationProps"})
public class AlertStorageObjectPayload {
    @JsonProperty
    private String expr;

    @JsonProperty("for")
    private int forTimes;

    @JsonProperty
    private HumanReadableDuration every = HumanReadableDuration.DURATION_1_MINUTE;

    /**
     * silence period in minute
     */
    @JsonProperty
    private HumanReadableDuration silence;

    @Deprecated
    @JsonProperty
    private List<String> notifications;

    @JsonProperty
    private NotificationProps notificationProps;
}
