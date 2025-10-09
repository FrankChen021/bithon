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

import com.google.common.collect.ImmutableMap;
import org.bithon.agent.rpc.brpc.cmd.IConfigurationCommand;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.service.common.calcite.SqlExecutionContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 1/3/23 8:18 pm
 */
public class ConfigurationTable extends AbstractBaseTable implements IPushdownPredicateProvider {

    public static class ConfigurationPropertyRecord {
        public String path;
        public String type;
        public boolean dynamic;
        public boolean required;
        public String value;
        public String defaultValue;
        public boolean changed;
        public String description;
        public String suggestion;
    }

    private final AgentServiceProxyFactory proxyFactory;

    public ConfigurationTable(AgentServiceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    protected List<Object[]> getData(SqlExecutionContext executionContext) {
        return proxyFactory.createBroadcastProxy(executionContext.getParameters(), IConfigurationCommand.class)
                           .getConfigurationProperties()
                           .stream()
                           .map((cfg) -> new Object[]
                               {
                                   cfg.getPath(),
                                   cfg.getType(),
                                   cfg.isDynamic(),
                                   cfg.isRequired(),
                                   cfg.getValue(),
                                   cfg.getDefaultValue(),
                                   cfg.isChanged(),
                                   cfg.getDescription(),
                                   cfg.getSuggestion()
                               })
                           .collect(Collectors.toList());
    }

    @Override
    protected Class<?> getRecordClazz() {
        return ConfigurationPropertyRecord.class;
    }

    @Override
    public Map<String, Boolean> getPredicates() {
        return ImmutableMap.of(IAgentControllerApi.PARAMETER_NAME_APP_NAME, true,
                               IAgentControllerApi.PARAMETER_NAME_INSTANCE, true);
    }
}
