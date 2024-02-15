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

package org.bithon.server.alerting.common.evaluator.metric.relative;

import org.bithon.component.commons.utils.HumanReadableDuration;

/**
 * Decreased by a percentage of an absolute value over a previous window
 *
 * @author frankchen
 * @date 2020-08-21 17:06:34
 */
public class RelativeLTPredicate extends AbstractRelativeThresholdPredicate {

    public RelativeLTPredicate(Number threshold,
                               HumanReadableDuration offset) {
        super(threshold, offset, false);
    }

    @Override
    protected boolean matches(double delta, double threshold) {
        return delta < threshold;
    }
}
