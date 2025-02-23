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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.bithon.server.alerting.common.evaluator.result.EvaluationStatus;

import java.util.List;

/**
 * @author Frank Chen
 * @date 19/3/22 8:00 PM
 */
@Data
public class ExpressionEvaluationResult {
    private EvaluationStatus result;
    private List<OutputMessage> outputs;

    @JsonCreator
    public ExpressionEvaluationResult(@JsonProperty("result") EvaluationStatus result,
                                      @JsonProperty("outputs") List<OutputMessage> outputs) {
        this.result = result;
        this.outputs = outputs;
    }
}
