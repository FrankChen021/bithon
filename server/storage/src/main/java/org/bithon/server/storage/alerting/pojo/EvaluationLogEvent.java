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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 10/4/22 7:15 PM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationLogEvent {
    private Timestamp timestamp;
    private long sequence;
    private String alertId;
    private String instance; // Map to the instance field in the db
    // INFO, ERROR
    private String level;
    private String clazz;
    private String message;
}
