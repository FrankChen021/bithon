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

package org.bithon.server.web.service.agent.sql;

import com.google.common.collect.ImmutableMap;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.bithon.server.discovery.client.ServiceBroadcastInvoker;
import org.bithon.server.web.service.agent.sql.table.AgentCommandFactory;
import org.bithon.server.web.service.agent.sql.table.ClassTable;
import org.bithon.server.web.service.agent.sql.table.ConfigurationTable;
import org.bithon.server.web.service.agent.sql.table.InstanceTable;
import org.bithon.server.web.service.agent.sql.table.InstrumentedMethodTable;
import org.bithon.server.web.service.agent.sql.table.LoggerTable;
import org.bithon.server.web.service.agent.sql.table.ThreadTable;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 1/3/23 2:35 pm
 */
public class AgentSchema extends AbstractSchema {
    private final ImmutableMap<String, Table> tableMap;

    public AgentSchema(ServiceBroadcastInvoker serviceInvoker, ApplicationContext applicationContext) {
        AgentCommandFactory agentCommandFactory = new AgentCommandFactory(serviceInvoker.getDiscoveryClient(),
                                                                          serviceInvoker.getExecutor(),
                                                                          applicationContext);
        this.tableMap = ImmutableMap.of("instance", new InstanceTable(serviceInvoker),
                                        "loaded_class", new ClassTable(agentCommandFactory),
                                        "thread", new ThreadTable(agentCommandFactory),
                                        "configuration", new ConfigurationTable(serviceInvoker),
                                        "instrumented_method", new InstrumentedMethodTable(serviceInvoker),
                                        "logger", new LoggerTable(serviceInvoker)
        );
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return tableMap;
    }

}
