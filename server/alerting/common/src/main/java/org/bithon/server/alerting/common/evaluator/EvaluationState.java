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

package org.bithon.server.alerting.common.evaluator;


import lombok.Getter;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.storage.alerting.pojo.AlertState;

/**
 * @author frank.chen021@outlook.com
 * @date 29/3/25 12:42 pm
 */
@Getter
public class EvaluationState {
    private AlertState.SeriesState series;
    private EvaluationOutputs outputs;

    public EvaluationState(AlertState.SeriesState series, EvaluationOutputs outputs) {
        this.series = series;
        this.outputs = outputs;
    }
}
