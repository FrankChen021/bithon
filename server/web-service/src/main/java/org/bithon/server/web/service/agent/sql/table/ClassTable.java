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
public class ClassTable extends AbstractBaseTable {
    private final AgentCommandFactory impl;

    public ClassTable(AgentCommandFactory impl) {
        this.impl = impl;
    }

    @Override
    protected List<IAgentCommandApi.IObjectArrayConvertable> getData(SqlExecutionContext executionContext) {
        return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>) impl.create(IAgentCommandApi.class, executionContext.getParameters(), IJvmCommand.class)
                                                                              .getLoadedClassList()
                                                                              .stream().map((clazzInfo) -> {
                    IAgentCommandApi.ClassRecord classRecord = new IAgentCommandApi.ClassRecord();
                    classRecord.name = clazzInfo.getName();
                    classRecord.classLoader = clazzInfo.getClassLoader();
                    classRecord.isAnnotation = clazzInfo.isAnnotation() ? 1 : 0;
                    classRecord.isInterface = clazzInfo.isInterface() ? 1 : 0;
                    classRecord.isEnum = clazzInfo.isEnum() ? 1 : 0;
                    classRecord.isSynthetic = clazzInfo.isSynthetic() ? 1 : 0;
                    return classRecord;
                })
                                                                              .collect(Collectors.toList());
    }

    @Override
    protected Class<?> getRecordClazz() {
        return IAgentCommandApi.ClassRecord.class;
    }
}
