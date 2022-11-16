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
    private final ScheduledExecutorService loaderScheduler;
    private final Map<String, DataSourceSchema> schemas = new ConcurrentHashMap<>();
    private long lastLoadAt;

    public DataSourceSchemaManager(ISchemaStorage schemaStorage) {
        this.schemaStorage = schemaStorage;
        loaderScheduler = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.of("schema-loader"));
    }

    public boolean addDataSourceSchema(DataSourceSchema schema) {
        if (schemas.putIfAbsent(schema.getName(), schema) == null) {
            if (!schema.isVirtual()) {
                try {
                    schemaStorage.putIfNotExist(schema.getName(), schema);
                } catch (IOException e) {
                    log.error("Can't save schema [{}} for: [{}]", schema.getName(), e.getMessage());
                    schemas.remove(schema.getName());
                    throw new RuntimeException(e);
                }
            }

            this.onChange(null, schema);
            return true;
        }
        return false;
    }

    public boolean containsSchema(String name) {
        return schemaStorage.containsSchema(name);
    }

    public void updateDataSourceSchema(DataSourceSchema schema) {
        try {
            this.schemaStorage.update(schema.getName(), schema);
        } catch (IOException e) {
            return;
        }
        this.onChange(this.schemas.put(schema.getName(), schema),
                      schema);
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

        this.schemas.forEach((name, schema) -> onChange(null, schema));
    }

    private void incrementalLoadSchemas() {
        try {
            List<DataSourceSchema> changedSchemaList = schemaStorage.getSchemas(this.lastLoadAt);

            log.info("{} Schemas has been changed since {}.", changedSchemaList.size(), DateTime.toYYYYMMDDhhmmss(this.lastLoadAt));

            for (DataSourceSchema changedSchema : changedSchemaList) {
                this.onChange(this.schemas.put(changedSchema.getName(), changedSchema), changedSchema);
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
                                               // no delay to execute the first task
                                               0,
                                               1,
                                               TimeUnit.MINUTES);
    }

    @Override
    public void destroy() {
        log.info("Shutting down Schema Manager...");
        loaderScheduler.shutdown();
    }

    private void onChange(DataSourceSchema oldSchema, DataSourceSchema dataSourceSchema) {
        for (IDataSourceSchemaListener listener : listeners) {
            try {
                listener.onChange(oldSchema, dataSourceSchema);
            } catch (Exception e) {
                log.error("notify onAdd exception", e);
            }
        }
    }

    public interface IDataSourceSchemaListener {
        void onChange(DataSourceSchema oldSchema, DataSourceSchema dataSourceSchema);
    }
}
