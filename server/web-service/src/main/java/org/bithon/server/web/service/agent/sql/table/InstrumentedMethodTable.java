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

import org.bithon.agent.rpc.brpc.cmd.IInstrumentationCommand;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 4/4/23 10:39 pm
 */
public class InstrumentedMethodTable extends AbstractBaseTable {
    private final AgentCommandFactory commandFactory;

    public InstrumentedMethodTable(AgentCommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<IAgentCommandApi.IObjectArrayConvertable> getData(SqlExecutionContext executionContext) {
        return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>)
                commandFactory.create(IAgentCommandApi.class,
                                      executionContext.getParameters(),
                                      IInstrumentationCommand.class)
                              .getInstrumentedMethods()
                              .stream()
                              .map((method) -> {
                                  IAgentCommandApi.InstrumentedMethodRecord record = new IAgentCommandApi.InstrumentedMethodRecord();
                                  record.clazzName = method.getClazzName();
                                  record.isStatic = method.isStatic();
                                  record.parameters = method.getParameters();
                                  record.methodName = method.getMethodName();
                                  record.returnType = method.getReturnType();
                                  record.interceptor = method.getInterceptor();
                                  return record;
                              }).collect(Collectors.toList());
    }

    @Override
    protected Class getRecordClazz() {
        return IAgentCommandApi.InstrumentedMethodRecord.class;
    }
}
