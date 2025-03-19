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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.alerting.Label;

import java.math.BigDecimal;

/**
 * @author frankchen
 * @date 2020-08-27 18:22:48
 */
@Data
public class RelativeComparisonEvaluationOutput implements IEvaluationOutput {

    private boolean isMatched;
    private BigDecimal base;
    private BigDecimal current;
    private double delta;
    private Number threshold;
    private long start;
    private long end;
    private Label label;

    @JsonCreator
    public RelativeComparisonEvaluationOutput(@JsonProperty boolean isMatched,
                                              @JsonProperty Label label,
                                              @JsonProperty BigDecimal base,
                                              @JsonProperty BigDecimal current,
                                              @JsonProperty double delta,
                                              @JsonProperty Number threshold,
                                              @JsonProperty long start,
                                              @JsonProperty long end) {
        this.isMatched = isMatched;
        this.label = label;
        this.base = base;
        this.current = current;
        this.delta = delta;
        this.threshold = threshold;
        this.start = start;
        this.end = end;
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getEnd() {
        return end;
    }

    @Override
    public Label getLabel() {
        return label;
    }

    @Override
    public String getThresholdText() {
        return threshold.toString();
    }

    @Override
    public String getCurrentText() {
        return StringUtils.format("%.2f], base [%.2f", current.doubleValue(), base.doubleValue());
    }

    @Override
    public String getDeltaText() {
        return threshold instanceof HumanReadablePercentage ? (delta * 100) + "%" : String.valueOf(delta);
    }
}
