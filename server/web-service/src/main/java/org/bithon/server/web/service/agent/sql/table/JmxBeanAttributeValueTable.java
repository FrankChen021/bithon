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
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;

import javax.management.openmbean.CompositeData;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/8 16:59
 */
public class JmxBeanAttributeValueTable extends AbstractBaseTable implements IPushdownPredicateProvider {
    private final AgentServiceProxyFactory proxyFactory;

    public JmxBeanAttributeValueTable(AgentServiceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    protected List<Object[]> getData(SqlExecutionContext executionContext) {
        String beanName = (String) executionContext.get("beanName");
        String attributeName = (String) executionContext.get("attribName");

        String val = proxyFactory.create(executionContext.getParameters(), IJvmCommand.class)
                                 .getBeanAttribute(beanName, attributeName);

        return Collections.singletonList(new Object[]{val});
    }

    public static class JmxBeanAttributeValue {
        public String value;
    }

    @Override
    protected Class<?> getRecordClazz() {
        return JmxBeanAttributeValue.class;
    }

    @Override
    public Map<String, Boolean> getPredicates() {
        return ImmutableMap.of(IAgentControllerApi.PARAMETER_NAME_APP_NAME, true,
                               IAgentControllerApi.PARAMETER_NAME_INSTANCE, true,
                               "beanName", true,
                               "attribName", true);
    }
}
