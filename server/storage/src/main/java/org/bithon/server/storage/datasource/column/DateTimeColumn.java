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

package org.bithon.server.storage.datasource.column;

import org.bithon.component.commons.expression.IDataType;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;

import javax.annotation.Nullable;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/20 09:40
 */
public class DateTimeColumn extends AbstractColumn {

    public DateTimeColumn(String name, @Nullable String alias) {
        super(name, alias);
    }

    @Override
    public IDataType getDataType() {
        return IDataType.DATETIME_3;
    }

    @Override
    public ResultColumn getResultColumn() {
        return new ResultColumn(getName());
    }
}