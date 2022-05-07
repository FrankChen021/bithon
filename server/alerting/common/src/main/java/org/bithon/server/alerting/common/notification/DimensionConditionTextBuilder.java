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

import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.matcher.BetweenMatcher;
import org.bithon.server.commons.matcher.IMatcherVisitor;
import org.bithon.server.commons.matcher.InMatcher;
import org.bithon.server.commons.matcher.StringAntPathMatcher;
import org.bithon.server.commons.matcher.StringContainsMatcher;
import org.bithon.server.commons.matcher.StringEndWithMatcher;
import org.bithon.server.commons.matcher.StringEqualMatcher;
import org.bithon.server.commons.matcher.StringIContainsMatcher;
import org.bithon.server.commons.matcher.StringNotEqualMatcher;
import org.bithon.server.commons.matcher.StringRegexMatcher;
import org.bithon.server.commons.matcher.StringStartsWithMatcher;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4
 */
public class DimensionConditionTextBuilder implements IMatcherVisitor<String> {
    private final DataSourceSchema dataSource;

    @Getter
    @Setter
    private String dimension;

    public DimensionConditionTextBuilder(String dimension, DataSourceSchema dataSource) {
        this.dimension = dimension;
        this.dataSource = dataSource;
    }

    @Override
    public String visit(StringEqualMatcher condition) {
        IDimensionSpec dimensionSpec = dataSource.getDimensionSpecByName(dimension);
        return StringUtils.format("%s[%s]", dimensionSpec.getDisplayText(), condition.getPattern());
    }

    @Override
    public String visit(StringAntPathMatcher antPathMatcher) {
        return null;
    }

    @Override
    public String visit(InMatcher matcher) {
        throw new RuntimeException("visit(InDimensionCondition condition) not implemented");
    }

    @Override
    public String visit(StringNotEqualMatcher matcher) {
        throw new RuntimeException("visit(NotEqualDimensionCondition matcher) not implemented");
    }

    @Override
    public String visit(StringStartsWithMatcher matcher) {
        IDimensionSpec dimensionSpec = dataSource.getDimensionSpecByName(dimension);
        return StringUtils.format("%s[以%s开始]", dimensionSpec.getDisplayText(), matcher.getPattern());
    }

    @Override
    public String visit(BetweenMatcher matcher) {
        return null;
    }

    @Override
    public String visit(StringEndWithMatcher matcher) {
        IDimensionSpec dimensionSpec = dataSource.getDimensionSpecByName(dimension);
        return StringUtils.format("%s[以%s结束]", dimensionSpec.getDisplayText(), matcher.getPattern());
    }

    @Override
    public String visit(StringIContainsMatcher iContainsMatcher) {
        return null;
    }

    @Override
    public String visit(StringContainsMatcher matcher) {
        IDimensionSpec dimensionSpec = dataSource.getDimensionSpecByName(dimension);
        return StringUtils.format("%s[包含%s]", dimensionSpec.getDisplayText(), matcher.getPattern());
    }

    @Override
    public String visit(StringRegexMatcher matcher) {
        IDimensionSpec dimensionSpec = dataSource.getDimensionSpecByName(dimension);
        return StringUtils.format("%s[类似%s]", dimensionSpec.getDisplayText(), matcher.getPattern());
    }
}
