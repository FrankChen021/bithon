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
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Frank Chen
 * @date 25/1/24 4:56 pm
 */
public class ProbabilisticSamplerTransform implements ITransformer {

    private final double fraction;

    @JsonCreator
    public ProbabilisticSamplerTransform(@JsonProperty("percentage") String percentage) {
        this.fraction = HumanReadablePercentage.parse(percentage).getFraction();
    }

    @Override
    public boolean transform(IInputRow data) {
        if (fraction <= 0) {
            return false;
        }
        if (fraction >= 1) {
            return true;
        }

        return ThreadLocalRandom.current().nextDouble() <= fraction;
    }
}
