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

import lombok.Data;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.commons.time.TimeSpan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author frankchen
 * @date 2020-08-27 18:22:48
 */
@Data
public class RelativeComparisonEvaluationOutput implements IEvaluationOutput {

    private boolean matches;
    private BigDecimal base;
    private BigDecimal now;
    private double delta;
    private Number threshold;
    private IMetricEvaluator metric;
    private TimeSpan start;
    private TimeSpan end;

    /**
     * Used for plotting if the image rendering service is enabled
     */
    private List<Map<String, Object>> baseline;

    @Override
    public TimeSpan getStart() {
        return start;
    }

    @Override
    public TimeSpan getEnd() {
        return end;
    }

    @Override
    public String getThresholdText() {
        return threshold.toString();
    }

    @Override
    public String getCurrentText() {
        return StringUtils.format("[%.2f], base [%.2f]", now.doubleValue(), base.doubleValue());
    }

    @Override
    public String getDeltaText() {
        return threshold instanceof HumanReadablePercentage ? (delta * 100) + "%" : String.valueOf(delta);
    }
}
