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

package org.bithon.server.alerting.manager.biz;

import com.fasterxml.jackson.databind.JsonNode;
import org.bithon.server.commons.time.Period;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.BooleanColumn;
import org.bithon.server.storage.datasource.column.DateTimeColumn;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/26 20:23
 */
public class AlertRuleSchema implements ISchema {

    private final Map<String, IColumn> columnMap = new LinkedHashMap<>();
    private final TimestampSpec timestampSpec = new TimestampSpec("createdAt");
    private final IDataStoreSpec dataStoreSpec;

    public AlertRuleSchema() {
        List<IColumn> columList = new ArrayList<>();
        columList.add(new StringColumn("alertId", "alertId"));
        columList.add(new StringColumn("name", "name"));
        columList.add(new StringColumn("appName", "appName"));
        columList.add(new BooleanColumn("enabled", "enabled"));
        columList.add(new DateTimeColumn("createdAt", "createdAt"));
        columList.add(new DateTimeColumn("updatedAt", "updatedAt"));
        columList.add(new DateTimeColumn("lastAlertAt", "lastAlertAt"));
        columList.add(new StringColumn("lastOperator", "lastOperator"));
        columList.add(new StringColumn("lastRecordId", "lastRecordId"));
        columList.add(new StringColumn("alertStatus", "alertStatus"));

        for (IColumn column : columList) {
            columnMap.put(column.getName(), column);
            if (column.getAlias() != null) {
                columnMap.put(column.getAlias(), column);
            }
        }

        this.dataStoreSpec = new IDataStoreSpec() {
            @Override
            public String getStore() {
                return "";
            }

            @Override
            public void setSchema(ISchema schema) {

            }

            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public IDataSourceReader createReader() throws IOException {
                return new IDataSourceReader() {
                    @Override
                    public List<Map<String, Object>> timeseries(Query query) {
                        return List.of();
                    }

                    @Override
                    public List<?> groupBy(Query query) {
                        return List.of();
                    }

                    @Override
                    public List<?> select(Query query) {
                        return List.of();
                    }

                    @Override
                    public int count(Query query) {
                        return 0;
                    }

                    @Override
                    public List<String> distinct(Query query) {
                        return List.of();
                    }
                };
            }
        };
    }

    @Override
    public String getName() {
        return "alert_rule";
    }

    @Override
    public String getDisplayText() {
        return "";
    }

    @Override
    public TimestampSpec getTimestampSpec() {
        return new TimestampSpec("createdAt");
    }

    @Override
    public IColumn getColumnByName(String name) {
        return this.columnMap.get(name);
    }

    @Override
    public Collection<IColumn> getColumns() {
        return this.columnMap.values();
    }

    @Override
    public JsonNode getInputSourceSpec() {
        return null;
    }

    @Override
    public IDataStoreSpec getDataStoreSpec() {
        return null;
    }

    @Override
    public ISchema withDataStore(IDataStoreSpec spec) {
        return null;
    }

    @Override
    public void setSignature(String signature) {
    }

    @Override
    public String getSignature() {
        return "";
    }

    @Override
    public Period getTtl() {
        return null;
    }
}
