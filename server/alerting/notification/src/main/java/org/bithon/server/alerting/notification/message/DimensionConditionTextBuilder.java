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

package org.bithon.server.alerting.notification.message;

import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.filter.IColumnFilter;
import org.bithon.server.storage.datasource.filter.IColumnFilterVisitor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4
 */
public class DimensionConditionTextBuilder implements IColumnFilterVisitor<String> {
    private final ISchema dataSource;

    @Getter
    @Setter
    private String dimension;

    public DimensionConditionTextBuilder(String dimension, ISchema dataSource) {
        this.dimension = dimension;
        this.dataSource = dataSource;
    }

    @Override
    public String visit(IColumnFilter.GreaterThanFilter filter) {
        IColumn dimensionSpec = dataSource.getColumnByName(dimension);
        return StringUtils.format("%s[%s]", dimensionSpec.getName(), filter.getExpected());
    }

    @Override
    public String visit(IColumnFilter.GreaterThanOrEqualFilter filter) {
        IColumn dimensionSpec = dataSource.getColumnByName(dimension);
        return StringUtils.format("%s[%s]", dimensionSpec.getName(), filter.getExpected());
    }

    @Override
    public String visit(IColumnFilter.EqualFilter filter) {
        IColumn dimensionSpec = dataSource.getColumnByName(dimension);
        return StringUtils.format("%s[%s]", dimensionSpec.getName(), filter.getExpected());
    }

    @Override
    public String visit(IColumnFilter.LessThanFilter filter) {
        IColumn dimensionSpec = dataSource.getColumnByName(dimension);
        return StringUtils.format("%s[%s]", dimensionSpec.getName(), filter.getExpected());
    }

    @Override
    public String visit(IColumnFilter.LessThanOrEqualFilter filter) {
        IColumn dimensionSpec = dataSource.getColumnByName(dimension);
        return StringUtils.format("%s[%s]", dimensionSpec.getName(), filter.getExpected());
    }

    @Override
    public String visit(IColumnFilter.NotEqualFilter filter) {
        IColumn dimensionSpec = dataSource.getColumnByName(dimension);
        return StringUtils.format("%s[%s]", dimensionSpec.getName(), filter.getExpected());
    }

}
