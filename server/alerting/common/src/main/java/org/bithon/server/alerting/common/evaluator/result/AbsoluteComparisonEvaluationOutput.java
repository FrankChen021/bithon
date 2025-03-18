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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.alerting.Label;

/**
 * @author frankchen
 * @date 2020-08-27 18:35:19
 */
public class AbsoluteComparisonEvaluationOutput implements IEvaluationOutput {

    @Getter
    private final boolean matched;

    /**
     * The value at the moment when the metric is retrieved
     */
    @Getter
    private final String current;

    @Getter
    private final String threshold;

    @Getter
    private final String delta;

    @Getter
    private final long start;

    @Getter
    private final long end;

    @Getter
    private final Label label;

    @JsonCreator
    public AbsoluteComparisonEvaluationOutput(@JsonProperty("start") long start,
                                              @JsonProperty("end") long end,
                                              @JsonProperty("label") Label label,
                                              @JsonProperty("current") String current,
                                              @JsonProperty("threshold") String threshold,
                                              @JsonProperty("delta") String delta,
                                              @JsonProperty("matched") boolean matched) {
        this.start = start;
        this.end = end;
        this.matched = matched;
        this.label = label;
        this.current = current;
        this.threshold = threshold;
        this.delta = delta;
    }

    @JsonIgnore
    @Override
    public String getThresholdText() {
        return threshold;
    }

    @JsonIgnore
    @Override
    public String getCurrentText() {
        return current;
    }

    @JsonIgnore
    @Override
    public String getDeltaText() {
        return delta;
    }

}
