/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.core.tracing.sampler;

import org.bithon.agent.core.utils.lang.MathUtils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A global sampling decision maker based on requests
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/9 10:55 下午
 */
public class PercentageSampler implements ISampler {
    private final int samplingRate;
    private final AtomicLong counter = new AtomicLong();

    /**
     * @param samplingRate (0, 100)
     */
    public PercentageSampler(int samplingRate) {
        if (samplingRate < 1 || samplingRate >= 100) {
            throw new IllegalArgumentException("samplingRate must be in the range of (0,100)");
        }

        this.samplingRate = samplingRate;
    }

    @Override
    public SamplingMode decideSamplingMode(Object request) {
        long reminder = MathUtils.floorMod(counter.addAndGet(this.samplingRate), 100);
        return reminder > 0 && reminder <= this.samplingRate ? SamplingMode.FULL : SamplingMode.NONE;
    }
}
