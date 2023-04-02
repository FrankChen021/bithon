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

import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/2 16:20
 */
public class LoggerTable extends AbstractBaseTable {
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
}
