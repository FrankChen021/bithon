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

package org.bithon.server.web.service.agent.sql.table;

import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/2 16:20
 */
public class LoggerTable extends AbstractBaseTable implements IUpdatableTable {
    private final IAgentCommandApi impl;

    public LoggerTable(IAgentCommandApi impl) {
        this.impl = impl;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<IAgentCommandApi.IObjectArrayConvertable> getData(SqlExecutionContext executionContext) {
        String appId = (String) executionContext.get("appId");
        Preconditions.checkNotNull(appId, "'appId' is missed in the query filter");

        ServiceResponse<IAgentCommandApi.LoggerConfigurationRecord> records = impl.getLoggerList(new CommandArgs<>(appId, null));
        if (records.getError() != null) {
            throw new RuntimeException(records.getError().toString());
        }

        return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>) records.getRows();
    }

    @Override
    protected Class<?> getRecordClazz() {
        return IAgentCommandApi.LoggerConfigurationRecord.class;
    }

    @Override
    public void update(SqlExecutionContext executionContext,
                       IExpression filterExpression,
                       Map<String, Object> newValues) {
        String appId = (String) executionContext.get("appId");
        Preconditions.checkNotNull(appId, "'appId' is missed in the query filter");

        String newLevel = (String) newValues.get("level");
        if (newLevel == null) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "'level' is not updated");
        }

        try {
            LoggingLevel.valueOf(newLevel);
        } catch (IllegalArgumentException ignored) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Given level [%s] is not a valid value", newLevel);
        }

        if (newValues.size() > 1) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Only 'level' is allowed to updated");
        }

        IAgentCommandApi.SetLoggerArgs args = new IAgentCommandApi.SetLoggerArgs();
        args.setNewValues(newValues);
        args.setCondition(filterExpression);
        impl.setLogger(new CommandArgs<>(appId, args));
    }
}
