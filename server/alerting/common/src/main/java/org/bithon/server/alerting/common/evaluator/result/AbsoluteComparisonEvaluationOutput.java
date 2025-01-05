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
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.commons.time.TimeSpan;

/**
 * @author frankchen
 * @date 2020-08-27 18:35:19
 */
public class AbsoluteComparisonEvaluationOutput implements IEvaluationOutput {

    @Getter
    private final boolean isMatched;

    /**
     * The value at the moment when the metric is retrieved
     */
    @Getter
    private final String now;

    @Getter
    private final String threshold;

    @Getter
    private final String delta;

    @Getter
    private final TimeSpan start;

    @Getter
    private final TimeSpan end;

    @Getter
    private final Label label;

    public AbsoluteComparisonEvaluationOutput(TimeSpan start,
                                              TimeSpan end,
                                              Label label,
                                              String now,
                                              String threshold,
                                              String delta,
                                              boolean isMatches) {
        this.start = start;
        this.end = end;
        this.isMatched = isMatches;
        this.label = label;
        this.now = now;
        this.threshold = threshold;
        this.delta = delta;
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public String getThresholdText() {
        return threshold;
    }

    @Override
    public String getCurrentText() {
        return now;
    }

    @Override
    public String getDeltaText() {
        return delta;
    }

}
