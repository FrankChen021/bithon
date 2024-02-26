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

import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.controller.IAgentProxyApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 1/3/23 8:18 pm
 */
public class InstanceTable extends AbstractBaseTable {
    private final IAgentProxyApi impl;

    public InstanceTable(DiscoveredServiceInvoker invoker) {
        this.impl = invoker.createBroadcastApi(IAgentProxyApi.class);
    }

    @Override
    protected List<Object[]> getData(SqlExecutionContext executionContext) {
        // The 'instance' can be NULL, if it's NULL, all records will be retrieved
        String instance = (String) executionContext.getParameters().get(IAgentProxyApi.PARAMETER_NAME_INSTANCE);

        ServiceResponse<IAgentProxyApi.AgentInstanceRecord> clients = impl.getAgentInstanceList(instance);
        if (clients.getError() != null) {
            throw new RuntimeException(clients.getError().toString());
        }

        return clients.getRows()
                      .stream()
                      .map(IAgentProxyApi.AgentInstanceRecord::toObjectArray)
                      .collect(Collectors.toList());
    }

    @Override
    protected Class<?> getRecordClazz() {
        return IAgentProxyApi.AgentInstanceRecord.class;
    }
}
