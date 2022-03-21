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

package org.bithon.server.metric;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.event.EventDataSourceSchema;
import org.bithon.server.metric.storage.ISchemaStorage;
import org.bithon.server.tracing.TraceDataSourceSchema;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frankchen
 * @date 2020-08-21 15:13:41
 */
@Slf4j
@Service
public class DataSourceSchemaManager implements SmartLifecycle {
    private final List<IDataSourceSchemaListener> listeners = new ArrayList<>();
    private final ISchemaStorage schemaStorage;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService loaderScheduler;
    private Map<String, DataSourceSchema> schemas = new ConcurrentHashMap<>();

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

        // load the definition in file
        try (InputStream is = this.getClass()
                                  .getClassLoader()
                                  .getResourceAsStream(String.format(Locale.ENGLISH, "schema/%s.json", name))) {
            if (is != null) {
                schema = objectMapper.readValue(is, DataSourceSchema.class);

                // save the definition in file into storage
                schemaStorage.putIfNotExist(name, schema);

                schemas.put(name, schema);

                return schema;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        throw new DataSourceNotFoundException("Can't find schema for datasource " + name);
    }

    public Map<String, DataSourceSchema> getDataSources() {
        return new HashMap<>(schemas);
    }

    public void addListener(IDataSourceSchemaListener listener) {
        listeners.add(listener);
    }

    private void loadSchema() {
        log.info("Loading metric schemas");
        try {
            schemas = schemaStorage.getSchemas().stream().collect(Collectors.toConcurrentMap(DataSourceSchema::getName, v -> v));
            schemas.put(TraceDataSourceSchema.getSchema().getName(), TraceDataSourceSchema.getSchema());
            schemas.put(EventDataSourceSchema.getSchema().getName(), EventDataSourceSchema.getSchema());
        } catch (Exception e) {
            log.error("Exception occurs when loading schemas", e);
        }
    }

    @Override
    public void start() {
        loaderScheduler.scheduleWithFixedDelay(this::loadSchema, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        loaderScheduler.shutdown();
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    public interface IDataSourceSchemaListener {
        void onRmv(DataSourceSchema dataSourceSchema);

        void onAdd(DataSourceSchema dataSourceSchema);
    }
}
