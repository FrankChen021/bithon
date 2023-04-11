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

import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 1/3/23 8:18 pm
 */
@SuppressWarnings({"unchecked"})
public class ThreadTable extends AbstractBaseTable {
    private final AgentCommandFactory commandFactory;

    public ThreadTable(AgentCommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    @Override
    protected List<IAgentCommandApi.IObjectArrayConvertable> getData(SqlExecutionContext executionContext) {
        String appId = (String) executionContext.get("appId");
        Preconditions.checkNotNull(appId, "'appId' is missed in the query filter");

        return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>)
                commandFactory.create(IAgentCommandApi.class,
                                      appId,
                                      IJvmCommand.class)
                              .dumpThreads()
                              .stream()
                              .map((threadInfo) -> {
                                  IAgentCommandApi.ThreadRecord thread = new IAgentCommandApi.ThreadRecord();
                                  thread.setName(threadInfo.getName());
                                  thread.setThreadId(threadInfo.getThreadId());
                                  thread.setState(threadInfo.getState());
                                  thread.setPriority(threadInfo.getPriority());
                                  thread.setCpuTime(threadInfo.getCpuTime());
                                  thread.setUserTime(threadInfo.getUserTime());
                                  thread.setDaemon(threadInfo.isDaemon() ? 1 : 0);
                                  thread.setWaitedCount(threadInfo.getWaitedCount());
                                  thread.setWaitedTime(threadInfo.getWaitedTime());
                                  thread.setBlockedCount(threadInfo.getBlockedCount());
                                  thread.setBlockedTime(threadInfo.getBlockedTime());
                                  thread.setLockName(threadInfo.getLockName());
                                  thread.setLockOwnerId(threadInfo.getLockOwnerId());
                                  thread.setLockOwnerName(threadInfo.getLockOwnerName());
                                  thread.setInNative(threadInfo.getInNative());
                                  thread.setSuspended(threadInfo.getSuspended());
                                  thread.setStack(threadInfo.getStacks());
                                  return thread;
                              })
                              .collect(Collectors.toList());
    }

    @Override
    protected Class<?> getRecordClazz() {
        return IAgentCommandApi.ThreadRecord.class;
    }
}
