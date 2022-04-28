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

package org.bithon.server.alerting.common.model;

import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterOrEqualMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterThanMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessOrEqualMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessThanMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.NullValueMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.relative.baseline.AbstractBaselineMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.relative.ringgrowth.AbstractRatioThresholdMetricCondition;

/**
 * @author frank.chen021@outlook.com
 */
public interface IMetricConditionVisitor<T> {
    T visit(AbstractRatioThresholdMetricCondition metric);

    T visit(AbstractBaselineMetricCondition metric);

    T visit(GreaterThanMetricCondition metric);

    T visit(GreaterOrEqualMetricCondition metric);

    T visit(LessOrEqualMetricCondition metric);

    T visit(LessThanMetricCondition metric);

    T visit(NullValueMetricCondition metric);
}
