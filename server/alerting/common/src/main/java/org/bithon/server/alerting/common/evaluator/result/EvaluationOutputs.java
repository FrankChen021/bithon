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

package org.bithon.server.alerting.common.evaluator.result;

import lombok.Getter;
import lombok.Setter;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/7 22:59
 */
public class EvaluationOutputs extends ArrayList<EvaluationOutput> {

    public static EvaluationOutputs of(EvaluationOutput output) {
        EvaluationOutputs outputs = new EvaluationOutputs();
        outputs.add(output);
        return outputs;
    }

    public static EvaluationOutputs empty() {
        return new EvaluationOutputs();
    }

    @Setter
    @Getter
    private AlertStatus status = AlertStatus.READY;

    @Getter
    @Setter
    private boolean isMatched;

    public EvaluationOutputs() {
    }

    public EvaluationOutputs(AlertStatus status) {
        this.status = status;
    }

    @Override
    public boolean add(EvaluationOutput output) {
        isMatched = isMatched || output.isMatched();
        return super.add(output);
    }

    @Override
    public void add(int index, EvaluationOutput output) {
        isMatched = isMatched || output.isMatched();
        super.add(index, output);
    }

    @Override
    public boolean addAll(Collection<? extends EvaluationOutput> c) {
        for (EvaluationOutput output : c) {
            add(output);
        }
        return true;
    }
}
