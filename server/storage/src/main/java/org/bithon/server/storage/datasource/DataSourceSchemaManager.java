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

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.concurrency.ScheduledExecutorServiceFactor;
import org.bithon.component.commons.time.DateTime;
import org.bithon.server.storage.meta.ISchemaStorage;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @date 2020-08-21 15:13:41
 */
@Slf4j
public class DataSourceSchemaManager implements SmartLifecycle {
    private final List<IDataSourceSchemaListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final ISchemaStorage schemaStorage;
    private ScheduledExecutorService loaderScheduler;
    private final Map<String, DataSourceSchema> schemas = new ConcurrentHashMap<>();
    private long lastLoadAt;

    public DataSourceSchemaManager(ISchemaStorage schemaStorage) {
        this.schemaStorage = schemaStorage;

        start();
    }

    public boolean addDataSourceSchema(DataSourceSchema schema) {
        if (schema != null &&
            schemas.putIfAbsent(schema.getName(), schema) == null) {
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
        this.onChange(this.put(schema.getName(), schema), schema);
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

        this.schemas.forEach((name, schema) -> listener.onChange(null, schema));
    }

    private void incrementalLoadSchemas() {
        List<DataSourceSchema> changedSchemaList = schemaStorage.getSchemas(this.lastLoadAt);

        log.info("{} schema(s) have been changed since {}.", changedSchemaList.size(), DateTime.toYYYYMMDDhhmmss(this.lastLoadAt));

        for (DataSourceSchema changedSchema : changedSchemaList) {
            this.onChange(this.put(changedSchema.getName(), changedSchema), changedSchema);
        }

        this.lastLoadAt = System.currentTimeMillis();
    }

    /**
     * for better debugging only
     */
    private DataSourceSchema put(String name, DataSourceSchema schema) {
        return this.schemas.put(name, schema);
    }

    private void onChange(DataSourceSchema oldSchema, DataSourceSchema dataSourceSchema) {
        // Copy to list first to avoid a concurrency problem
        IDataSourceSchemaListener[] listenerList = this.listeners.toArray(new IDataSourceSchemaListener[0]);

        for (IDataSourceSchemaListener listener : listenerList) {
            try {
                listener.onChange(oldSchema, dataSourceSchema);
            } catch (Exception e) {
                log.error("notify onAdd exception", e);
            }
        }
    }

    @Override
    public void start() {
        log.info("Starting schema incremental loader...");
        loaderScheduler = ScheduledExecutorServiceFactor.newSingleThreadScheduledExecutor(NamedThreadFactory.of("schema-loader"));
        loaderScheduler.scheduleWithFixedDelay(this::incrementalLoadSchemas,
                                               // no delay to execute the first task
                                               0,
                                               1,
                                               TimeUnit.MINUTES);

        // Wait until the load complete
        // Not able to use the Future object returned by the scheduleWithFixedDelay above because the 'get'
        // works abnormally as its javadoc says
        int count = 0;
        while (this.lastLoadAt == 0 && count++ < 30) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        if (this.lastLoadAt == 0) {
            log.error("!!!!!!Timeout to wait for the first loading of schemas!!!");
        }
    }

    @Override
    public void stop() {
        if (loaderScheduler != null) {
            log.info("Shutting down Schema Manager...");
            loaderScheduler.shutdownNow();
            try {
                loaderScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                loaderScheduler = null;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return loaderScheduler != null && !loaderScheduler.isShutdown();
    }

    public interface IDataSourceSchemaListener {
        void onChange(DataSourceSchema oldSchema, DataSourceSchema dataSourceSchema);
    }
}
