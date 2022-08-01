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

package org.bithon.server.storage.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.server.storage.meta.ISchemaStorage;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @date 2020-08-21 15:13:41
 */
@Slf4j
public class DataSourceSchemaManager implements InitializingBean, DisposableBean {
    private final List<IDataSourceSchemaListener> listeners = new ArrayList<>();
    private final ISchemaStorage schemaStorage;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService loaderScheduler;
    private final Map<String, DataSourceSchema> schemas = new ConcurrentHashMap<>();
    private long lastLoadAt;

    public DataSourceSchemaManager(ISchemaStorage schemaStorage, ObjectMapper objectMapper) {
        this.schemaStorage = schemaStorage;
        this.objectMapper = objectMapper;
        loaderScheduler = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.of("schema-loader"));
    }

    public boolean addDataSourceSchema(DataSourceSchema schema) {
        if (schemas.putIfAbsent(schema.getName(), schema) == null) {
            try {
                schemaStorage.putIfNotExist(schema.getName(), schema);
            } catch (IOException e) {
                log.error("Can't save schema [{}} for: [{}]", schema.getName(), e.getMessage());
                schemas.remove(schema.getName());
                throw new RuntimeException(e);
            }

            for (IDataSourceSchemaListener listener : listeners) {
                try {
                    listener.onAdd(schema);
                } catch (Exception e) {
                    log.error("notify onAdd exception", e);
                }
            }
            return true;
        }
        return false;
    }

    public void rmvDataSourceSchema(DataSourceSchema schema) {
        schemas.remove(schema.getName());
        for (IDataSourceSchemaListener listener : listeners) {
            try {
                listener.onRmv(schema);
            } catch (Exception e) {
                log.error("notify onRmv exception", e);
            }
        }
    }

    public void updateDataSourceSchema(DataSourceSchema schema) {
        try {
            this.schemaStorage.update(schema.getName(), schema);
        } catch (IOException e) {
            return;
        }
        this.schemas.put(schema.getName(), schema);
    }

    public DataSourceSchema getDataSourceSchema(String name) {
        // load from cache first
        DataSourceSchema schema = schemas.get(name);
        if (schema != null) {
            return schema;
        }

        // load from the storage
        schema = schemaStorage.getSchemaByName(name);
        if (schema != null) {
            schemas.put(name, schema);
            return schema;
        }

        throw new DataSourceNotFoundException(name);
    }

    public Map<String, DataSourceSchema> getDataSources() {
        return new TreeMap<>(schemas);
    }

    public void addListener(IDataSourceSchemaListener listener) {
        listeners.add(listener);
    }

    private void incrementalLoadSchemas() {
        try {
            List<DataSourceSchema> changedSchemaList = schemaStorage.getSchemas(this.lastLoadAt);

            log.info("{} Schemas has been changed since {}.", changedSchemaList.size(), DateTime.toYYYYMMDDhhmmss(this.lastLoadAt));

            for (DataSourceSchema changedSchema : changedSchemaList) {

                DataSourceSchema schemaBeforeChange = this.schemas.get(changedSchema.getName());
                if (schemaBeforeChange != null
                    && Objects.equals(schemaBeforeChange.getSignature(), changedSchema.getSignature())) {
                    // same signature, do nothing
                    continue;
                }

                // stop input
                if (schemaBeforeChange != null && schemaBeforeChange.getInputSourceSpec() != null) {
                    log.info("Stop input source for schema [{}]", schemaBeforeChange.getName());
                    schemaBeforeChange.getInputSourceSpec().stop();
                }

                // start for the new schema
                if (changedSchema.getInputSourceSpec() != null) {
                    log.info("Start input source for schema [{}]", changedSchema.getName());
                    changedSchema.getInputSourceSpec().start(changedSchema);
                }

                this.schemas.put(changedSchema.getName(), changedSchema);
            }

            this.lastLoadAt = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Exception occurs when loading schemas", e);
        }
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Starting schema incremental loader...");
        loaderScheduler.scheduleWithFixedDelay(this::incrementalLoadSchemas,
                                               0, // no delay to execute the first task
                                               1,
                                               TimeUnit.MINUTES);
    }

    @Override
    public void destroy() {
        log.info("Shutting down Schema Manager...");
        loaderScheduler.shutdown();
    }

    public interface IDataSourceSchemaListener {
        void onRmv(DataSourceSchema dataSourceSchema);

        void onAdd(DataSourceSchema dataSourceSchema);
    }
}
