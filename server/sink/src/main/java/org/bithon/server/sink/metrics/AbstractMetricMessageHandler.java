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

package org.bithon.server.sink.metrics;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.datasource.input.Measurement;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/3 11:17 下午
 */
@Slf4j
@Getter
public abstract class AbstractMetricMessageHandler {

    private final DataSourceSchema schema;
    private final DataSourceSchema endpointSchema;
    private final IMetaStorage metaStorage;
    private final IMetricWriter metricStorageWriter;
    private final IMetricWriter endpointMetricStorageWriter;

    public AbstractMetricMessageHandler(String dataSourceName,
                                        IMetaStorage metaStorage,
                                        IMetricStorage metricStorage,
                                        DataSourceSchemaManager dataSourceSchemaManager) throws IOException {

        this.schema = dataSourceSchemaManager.getDataSourceSchema(dataSourceName);
        this.metaStorage = metaStorage;
        this.metricStorageWriter = metricStorage.createMetricWriter(schema);

        this.endpointSchema = dataSourceSchemaManager.getDataSourceSchema("topo-metrics");
        this.endpointSchema.setEnforceDuplicationCheck(false);
        this.endpointMetricStorageWriter = metricStorage.createMetricWriter(endpointSchema);
    }

    public String getType() {
        return this.schema.getName();
    }

    protected boolean beforeProcess(IInputRow message) throws Exception {
        return true;
    }

    public final void process(IteratorableCollection<IInputRow> metricMessages) {
        if (!metricMessages.hasNext()) {
            return;
        }

        MetricsAggregator endpointDataSource = new MetricsAggregator(this.endpointSchema, 60);

        //
        // convert
        //
        List<IInputRow> inputRowList = new ArrayList<>(8);
        while (metricMessages.hasNext()) {
            IInputRow metricMessage = metricMessages.next();

            try {
                if (!beforeProcess(metricMessage)) {
                    continue;
                }

                // extract endpoint
                endpointDataSource.aggregate(extractEndpointLink(metricMessage));

                processMeta(metricMessage);

                inputRowList.add(metricMessage);
            } catch (Exception e) {
                log.error("Failed to process metric object. dataSource=[{}], message=[{}] due to {}",
                          this.schema.getName(),
                          metricMessage,
                          e);
            }
        }

        //
        // save endpoint metrics in batch
        //
        try {
            this.endpointMetricStorageWriter.write(endpointDataSource.getRows());
        } catch (IOException e) {
            log.error("save metrics", e);
        }

        //
        // save metrics in batch
        //
        try {
            this.metricStorageWriter.write(inputRowList);
        } catch (IOException e) {
            log.error("Failed to save metrics [dataSource={}] due to: {}",
                      this.schema.getName(),
                      e);
        }
    }

    protected Measurement extractEndpointLink(IInputRow message) {
        return null;
    }

    private void processMeta(IInputRow metric) {
        Object appType = metric.getCol("appType");
        if (appType == null) {
            log.warn("Saving meta for [{}] failed due to lack of appType", this.schema.getName());
            return;
        }

        String appName = metric.getColAsString("appName");
        String instanceName = metric.getColAsString("instanceName");
        try {
            metaStorage.saveApplicationInstance(appName,
                                                appType.toString(),
                                                instanceName);
        } catch (Exception e) {
            log.error("Failed to save app info[appName={}, instance={}] due to: {}",
                      appName,
                      instanceName,
                      e);
        }
    }
}
