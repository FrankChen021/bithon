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

package org.bithon.agent.observability.tracing.sampler;

import org.bithon.agent.observability.tracing.config.TraceSamplingConfig;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/7 18:53
 */
public class PercentageSamplerTest {

    private ISampler createSampler(String percentageText) {
        TraceSamplingConfig config = new TraceSamplingConfig();
        config.setSamplingPercentage(new HumanReadablePercentage(percentageText));
        return new PercentageSampler(config);
    }

    @Test
    public void test_Percentage0() {
        ISampler sampler = createSampler("0%");

        for (int i = 0; i < 100; i++) {
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
        }
    }

    @Test
    public void test_Percentage_Minimum() {
        ISampler sampler = createSampler("0.001%");

        Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
        for (int i = 0; i < TraceSamplingConfig.PRECISION_MULTIPLIER - 1; i++) {
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
        }
        Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
    }

    @Test
    public void test_Percentage1() {
        ISampler sampler = createSampler("1%");

        Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
        for (int i = 0; i < 99; i++) {
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
        }
        Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
    }

    @Test
    public void test_Percentage25() {
        ISampler sampler = createSampler("25%");

        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
        }
    }

    @Test
    public void test_Percentage33() {
        ISampler sampler = createSampler("33%");

        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
        }
    }

    @Test
    public void test_Percentage50() {
        ISampler sampler = createSampler("50%");

        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
        }
    }

    @Test
    public void test_Percentage75() {
        ISampler sampler = createSampler("75%");

        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
            Assertions.assertEquals(SamplingMode.NONE, sampler.decideSamplingMode(null));
        }
    }

    @Test
    public void test_Percentage100() {
        ISampler sampler = createSampler("100%");

        for (int i = 0; i < 101; i++) {
            Assertions.assertEquals(SamplingMode.FULL, sampler.decideSamplingMode(null));
        }
    }
}
