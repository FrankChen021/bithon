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

package org.bithon.server.web.service.agent.service;

import com.google.common.collect.ImmutableMap;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.schema.impl.AbstractTable;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.common.sql.QueryContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 1/3/23 2:35 pm
 */
public class AgentSchema extends AbstractSchema {
    private final ImmutableMap<String, Table> tableMap;

    public AgentSchema(IAgentCommandApi impl) {
        this.tableMap = ImmutableMap.of("instance", new InstanceTable(impl),
                                        "loaded_class", new ClassTable(impl),
                                        "stack_trace", new StackTraceTable(impl),
                                        "configuration", new ConfigurationTable(impl));
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return tableMap;
    }

    @SuppressWarnings("rawtypes")
    private abstract static class BaseTable extends AbstractTable implements ScannableTable {
        private RelDataType rowType;

        protected abstract Class getRecordClazz();

        @Override
        public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
            if (rowType == null) {
                rowType = typeFactory.createJavaType(getRecordClazz());
            }
            return rowType;
        }

        @Override
        public Enumerable<Object[]> scan(final DataContext root) {
            return Linq4j.asEnumerable(getData((QueryContext) root).stream()
                                                                   .map((IAgentCommandApi.IObjectArrayConvertable::toObjectArray))
                                                                   .collect(Collectors.toList()));
        }

        protected abstract <T extends IAgentCommandApi.IObjectArrayConvertable> List<T> getData(QueryContext queryContext);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class InstanceTable extends BaseTable {
        private final IAgentCommandApi impl;

        InstanceTable(IAgentCommandApi impl) {
            this.impl = impl;
        }

        @Override
        protected List<IAgentCommandApi.IObjectArrayConvertable> getData(QueryContext queryContext) {
            ServiceResponse<IAgentCommandApi.InstanceRecord> clients = impl.getClients();
            if (clients.getError() != null) {
                throw new RuntimeException(clients.getError().toString());
            }
            return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>) clients.getRows();
        }

        @Override
        protected Class getRecordClazz() {
            return IAgentCommandApi.InstanceRecord.class;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class ClassTable extends BaseTable {
        private final IAgentCommandApi impl;

        ClassTable(IAgentCommandApi impl) {
            this.impl = impl;
        }

        @Override
        protected List<IAgentCommandApi.IObjectArrayConvertable> getData(QueryContext queryContext) {
            String appId = (String) queryContext.get("appId");

            ServiceResponse<IAgentCommandApi.ClassRecord> classList = impl.getClass(new CommandArgs<>(appId, null));
            if (classList.getError() != null) {
                throw new RuntimeException(classList.getError().toString());
            }

            return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>) classList.getRows();
        }

        @Override
        protected Class getRecordClazz() {
            return IAgentCommandApi.ClassRecord.class;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class StackTraceTable extends BaseTable {
        private final IAgentCommandApi impl;

        StackTraceTable(IAgentCommandApi impl) {
            this.impl = impl;
        }

        @Override
        protected List<IAgentCommandApi.IObjectArrayConvertable> getData(QueryContext queryContext) {
            String appId = (String) queryContext.get("appId");

            ServiceResponse<IAgentCommandApi.StackTraceRecord> stackTraceList = impl.getStackTrace(new CommandArgs<>(appId, null));
            if (stackTraceList.getError() != null) {
                throw new RuntimeException(stackTraceList.getError().toString());
            }

            return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>) stackTraceList.getRows();
        }

        @Override
        protected Class getRecordClazz() {
            return IAgentCommandApi.StackTraceRecord.class;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class ConfigurationTable extends BaseTable {
        private final IAgentCommandApi impl;

        ConfigurationTable(IAgentCommandApi impl) {
            this.impl = impl;
        }

        @Override
        protected List<IAgentCommandApi.IObjectArrayConvertable> getData(QueryContext queryContext) {
            String appId = (String) queryContext.get("appId");

            ServiceResponse<IAgentCommandApi.ConfigurationRecord> configurations = impl.getConfiguration(new CommandArgs<>(appId, null));
            if (configurations.getError() != null) {
                throw new RuntimeException(configurations.getError().toString());
            }

            return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>) configurations.getRows();
        }

        @Override
        protected Class getRecordClazz() {
            return IAgentCommandApi.ConfigurationRecord.class;
        }
    }
}
