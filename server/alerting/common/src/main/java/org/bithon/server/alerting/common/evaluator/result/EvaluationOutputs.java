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

import java.util.ArrayList;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/7 22:59
 */
public class EvaluationOutputs extends ArrayList<IEvaluationOutput> {
    public static final EvaluationOutputs EMPTY = new EvaluationOutputs();

    private boolean isMatched;

    public EvaluationOutputs() {
    }

    public EvaluationOutputs(IEvaluationOutput output) {
        this.add(output);
    }

    public boolean isMatched() {
        return isMatched;
    }

    @Override
    public boolean add(IEvaluationOutput output) {
        isMatched = isMatched || output.isMatched();
        return super.add(output);
    }

    @Override
    public void add(int index, IEvaluationOutput output) {
        isMatched = isMatched || output.isMatched();
        super.add(index, output);
    }
}
