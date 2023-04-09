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
import org.bithon.server.discovery.client.ServiceBroadcastInvoker;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;

import java.util.List;

/**
 * @author Frank Chen
 * @date 1/3/23 8:18 pm
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ConfigurationTable extends AbstractBaseTable {
    private final IAgentCommandApi impl;

    public ConfigurationTable(ServiceBroadcastInvoker impl) {
        this.impl = impl.create(IAgentCommandApi.class);
    }

    @Override
    protected List<IAgentCommandApi.IObjectArrayConvertable> getData(SqlExecutionContext executionContext) {
        String appId = (String) executionContext.get("appId");
        Preconditions.checkNotNull(appId, "'appId' is missed in the query filter");

        ServiceResponse<IAgentCommandApi.ConfigurationRecord> configurations = impl.getConfiguration(new CommandArgs<>(appId));
        if (configurations.getError() != null) {
            throw new RuntimeException(configurations.getError().toString());
        }

        return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>) configurations.getRows();
    }

    @Override
    protected Class<?> getRecordClazz() {
        return IAgentCommandApi.ConfigurationRecord.class;
    }
}
