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
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/12 21:59
 */
public class AssemblyTable extends AbstractBaseTable implements IPushdownPredicateProvider {

    private final AgentServiceProxyFactory proxyFactory;

    public AssemblyTable(AgentServiceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    protected Class<?> getRecordClazz() {
        return Record.class;
    }

    @Override
    protected List<Object[]> getData(SqlExecutionContext executionContext) {
        IJvmCommand jvmCommand = proxyFactory.create(executionContext.getParameters(),
                                                     IJvmCommand.class);

        String className = (String) executionContext.get("class");
        Preconditions.checkNotNull(className, "'class' filter is not given.");

        return jvmCommand.getAssemblyCode(className)
                         .stream()
                         .map((assembly) -> new Object[]{assembly})
                         .collect(Collectors.toList());
    }

    @Override
    public Map<String, Boolean> getPredicates() {
        return ImmutableMap.of(IAgentControllerApi.PARAMETER_NAME_APP_NAME, true,
                               IAgentControllerApi.PARAMETER_NAME_INSTANCE, true,
                               "class", true);
    }

    static class Record {
        public String code;
    }
}
