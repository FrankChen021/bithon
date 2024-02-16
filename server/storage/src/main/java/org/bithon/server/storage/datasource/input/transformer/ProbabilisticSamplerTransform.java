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

package org.bithon.server.storage.datasource.input.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.NumberUtils;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Use a determinant way instead of a random value-based way for probability calculation.
 * This way makes sure the UT is not flaky.
 *
 * @author Frank Chen
 * @date 25/1/24 4:56 pm
 */
public class ProbabilisticSamplerTransform implements ITransformer {

    @Getter
    private final HumanReadablePercentage percentage;

    // Runtime properties
    private final AtomicLong counter;
    private final long probability;

    /**
     * The minimum probability percentage is 0.001%, that is 0.00001.
     * During calculation, the probability is an integer-based value, starting from 1.
     * So, 0.001% * MAX_PROBABILITY_VALUE = 1
     */
    public final static long MAX_PROBABILITY_VALUE = 100_000;

    /**
     * @param percentage The minimum is 0.001%, that is 0.00001.
     *                   There's no special reason that why this minimum is chosen, just a value limitation here.
     */
    @JsonCreator
    public ProbabilisticSamplerTransform(@JsonProperty("percentage") HumanReadablePercentage percentage) {
        this.percentage = percentage;
        this.probability = (long) (percentage.getFraction() * MAX_PROBABILITY_VALUE);
        this.counter = new AtomicLong();
    }

    @Override
    public boolean transform(IInputRow data) {
        if (probability <= 0) {
            return false;
        }
        if (probability >= MAX_PROBABILITY_VALUE) {
            return true;
        }
        long reminder = NumberUtils.floorMod(counter.addAndGet(probability), MAX_PROBABILITY_VALUE);
        return reminder > 0 && reminder <= probability;
    }
}
