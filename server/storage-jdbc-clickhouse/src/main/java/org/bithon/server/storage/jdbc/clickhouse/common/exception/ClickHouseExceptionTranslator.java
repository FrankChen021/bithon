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

package org.bithon.server.storage.jdbc.clickhouse.common.exception;

import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;

import java.sql.SQLException;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/9 21:56
 */
public class ClickHouseExceptionTranslator extends DefaultExecuteListener {
    @Override
    public void exception(ExecuteContext ctx) {
        SQLException exception = ctx.sqlException();
        if (exception == null) {
            return;
        }

        // Do nothing but turn SQLException into a RuntimeException
        ctx.exception(new ClickHouseAccessException(exception.getErrorCode(),
                                                    exception.getMessage(),
                                                    exception));
    }
}
