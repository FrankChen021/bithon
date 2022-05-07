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

package org.bithon.server.alerting.common.notification;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterOrEqualMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterThanMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessOrEqualMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessThanMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.NullValueMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.relative.baseline.AbstractBaselineMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.relative.ringgrowth.AbstractRatioThresholdMetricCondition;
import org.bithon.server.alerting.common.model.IMetricConditionVisitor;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.spec.IMetricSpec;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4
 */
public class MetricConditionTextBuilder implements IMetricConditionVisitor<String> {

    private final DataSourceSchema dataSource;

    public MetricConditionTextBuilder(DataSourceSchema dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String visit(AbstractRatioThresholdMetricCondition metric) {
        IMetricSpec spec = dataSource.getMetricSpecByName(metric.getName());
        return StringUtils.format("[%s]环比%d分钟前%s%d%%",
                             spec.getDisplayText(),
                             metric.getMinute(),
                                  metric.isUp() ? "上涨" : "下跌",
                             metric.getPercentage());
    }

    @Override
    public String visit(AbstractBaselineMetricCondition metric) {
        IMetricSpec spec = dataSource.getMetricSpecByName(metric.getName());
        return StringUtils.format("[%s]同比%d天前%s%d%%",
                             spec.getDisplayText(),
                             metric.getDayOffset(),
                             metric.isPositive() ? "上涨" : "下跌",
                             metric.getPercentage());
    }

    @Override
    public String visit(GreaterThanMetricCondition metric) {
        IMetricSpec spec = dataSource.getMetricSpecByName(metric.getName());
        return StringUtils.format("%s > %s%s", spec.getDisplayText(),
                             metric.getExpected().toString(),
                             spec.getUnit());
    }

    @Override
    public String visit(GreaterOrEqualMetricCondition metric) {
        IMetricSpec spec = dataSource.getMetricSpecByName(metric.getName());
        return StringUtils.format("%s >= %s%s", spec.getDisplayText(),
                                  metric.getExpected().toString(),
                                  spec.getUnit());
    }

    @Override
    public String visit(LessOrEqualMetricCondition metric) {
        IMetricSpec spec = dataSource.getMetricSpecByName(metric.getName());
        return StringUtils.format("%s <= %s%s", spec.getDisplayText(),
                             metric.getExpected().toString(),
                             spec.getUnit());
    }

    @Override
    public String visit(LessThanMetricCondition metric) {
        IMetricSpec spec = dataSource.getMetricSpecByName(metric.getName());
        return StringUtils.format("%s < %s%s", spec.getDisplayText(),
                             metric.getExpected().toString(),
                             spec.getUnit());
    }

    @Override
    public String visit(NullValueMetricCondition metric) {
        IMetricSpec spec = dataSource.getMetricSpecByName(metric.getName());
        return StringUtils.format("%s has null value", spec.getDisplayText());
    }
}
