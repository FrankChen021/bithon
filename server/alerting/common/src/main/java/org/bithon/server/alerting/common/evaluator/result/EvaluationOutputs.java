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

import org.bithon.server.storage.alerting.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/7 22:59
 */
public class EvaluationOutputs extends ArrayList<EvaluationOutput> {
    public static final EvaluationOutputs EMPTY = new EvaluationOutputs();

    public static EvaluationOutputs of(EvaluationOutput output) {
        EvaluationOutputs outputs = new EvaluationOutputs();
        outputs.add(output);
        return outputs;
    }

    private boolean isMatched;

    public EvaluationOutputs() {
    }

    public boolean isMatched() {
        return isMatched;
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

    public EvaluationOutput last() {
        return this.get(this.size() - 1);
    }

    /**
     * Join two outputs by {@link Label}
     */
    public EvaluationOutputs intersect(EvaluationOutputs rhs) {
        if (rhs.isEmpty()) {
            return EvaluationOutputs.EMPTY;
        }
        if (this.size() > rhs.size()) {
            return rhs.intersect(this);
        }

        EvaluationOutputs result = new EvaluationOutputs();

        // Create a map from the right side outputs for quick lookup by label
        Map<Label, EvaluationOutput> rhsMap = new HashMap<>();
        for (EvaluationOutput output : rhs) {
            rhsMap.put(output.getLabel(), output);
        }

        // Perform left join - iterate through left outputs
        for (EvaluationOutput leftOutput : this) {
            Label label = leftOutput.getLabel();

            EvaluationOutput rightOutput = rhsMap.get(label);
            if (rightOutput != null) {
                // Found a match - add the left output to the result
                // (We could merge properties from both outputs here if needed)
                result.add(leftOutput);
            }
        }

        return result;
    }

    public Map<String, EvaluationOutputs> toMap() {
        Map<String, EvaluationOutputs> result = new HashMap<>();
        for (EvaluationOutput output : this) {
            result.computeIfAbsent(output.getExpressionId(), (k) -> new EvaluationOutputs()).add(output);
        }
        return result;
    }
}
