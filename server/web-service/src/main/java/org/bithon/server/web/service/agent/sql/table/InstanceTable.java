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
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.service.common.calcilte.SqlExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author Frank Chen
 * @date 1/3/23 8:18 pm
 */
public class InstanceTable extends AbstractBaseTable implements IPushdownPredicateProvider {
    private final DiscoveredServiceInvoker invoker;

    public InstanceTable(DiscoveredServiceInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    protected List<Object[]> getData(SqlExecutionContext executionContext) {
        // The 'instance' can be NULL, if it's NULL, all records will be retrieved
        String agentInstance = (String) executionContext.getParameters().get(IAgentControllerApi.PARAMETER_NAME_INSTANCE);

        List<DiscoveredServiceInstance> instanceList = invoker.getInstanceList(IAgentControllerApi.class);

        List<Object[]> result = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(instanceList.size());

        for (DiscoveredServiceInstance instance : instanceList) {
            invoker.getExecutor()
                   .submit(() -> this.invoker.createUnicastApi(IAgentControllerApi.class, () -> instance)
                                             .getAgentInstanceList(null, agentInstance))
                   .thenAccept((returning) -> {
                       List<Object[]> objectLists = returning.stream()
                                                             .map(IAgentControllerApi.AgentInstanceRecord::toObjectArray)
                                                             .toList();
                       synchronized (result) {
                           result.addAll(objectLists);
                       }
                   })
                   .whenComplete((ret, ex) -> countDownLatch.countDown());
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    protected Class<?> getRecordClazz() {
        return IAgentControllerApi.AgentInstanceRecord.class;
    }

    @Override
    public Map<String, Boolean> getPredicates() {
        // APP_NAME is NOT push down so that it supports more operators such as LIKE for better analysis support
        return ImmutableMap.of(IAgentControllerApi.PARAMETER_NAME_INSTANCE, false);
    }
}
