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

package org.bithon.agent.observability.tracing.config;

import org.bithon.agent.configuration.validation.Range;
import org.bithon.component.commons.utils.HumanReadablePercentage;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/7 20:27
 */
public class TraceSamplingConfig {
    /**
     * in range of [0, 100]
     * This is only kept for backward compatibility
     */
    @Deprecated()
    @Range(min = 0, max = 100)
    private int samplingRate = 0;

    public int getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
        this.setSamplingPercentage(new HumanReadablePercentage(samplingRate / 100.0));
    }

    /**
     * The minimum is 0.001%, that is 0.00001
     */
    public static final int PRECISION_MULTIPLIER = 100_000;

    /**
     * Supported precision: [0.001%, 100%]
     * The precision is limited because we're using a more definitive way to calculate if a request should be sampled.
     */
    private HumanReadablePercentage samplingPercentage = new HumanReadablePercentage(0);

    /**
     * A runtime value calculated by the percentage above
     */
    private int rate;

    public HumanReadablePercentage getSamplingPercentage() {
        return samplingPercentage;
    }

    public void setSamplingPercentage(HumanReadablePercentage samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
        this.rate = (int) (samplingPercentage.doubleValue() * PRECISION_MULTIPLIER);
    }

    public int getRate() {
        return rate;
    }
}
