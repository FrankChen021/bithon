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
import org.bithon.server.commons.time.TimeSpan;
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
public class SchemaManager implements SmartLifecycle {
    private final List<ISchemaChangedListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final ISchemaStorage schemaStorage;
    private ScheduledExecutorService loaderScheduler;
    private final Map<String, ISchema> schemas = new ConcurrentHashMap<>();
    private long lastLoadAt;

    public SchemaManager(ISchemaStorage schemaStorage) {
        this.schemaStorage = schemaStorage;
    }

    public boolean addSchema(ISchema schema) {
        return addSchema(schema, true);
    }

    public boolean addSchema(ISchema schema, boolean saveSchema) {
        if (schema != null &&
            schemas.putIfAbsent(schema.getName(), schema) == null) {
            if (saveSchema && !schema.isVirtual()) {
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

    public void updateSchema(ISchema schema) {
        try {
            this.schemaStorage.update(schema.getName(), schema);
        } catch (IOException e) {
            return;
        }
        this.onChange(this.put(schema.getName(), schema), schema);
    }

    public ISchema getSchema(String name) {
        return getSchema(name, true);
    }

    public ISchema getSchema(String name, boolean throwIfNotFound) {
        // load from cache first
        ISchema schema = schemas.get(name);
        if (schema != null) {
            return schema;
        }

        // load from the storage
        schema = schemaStorage.getSchemaByName(name);
        if (schema != null) {
            schemas.put(name, schema);
            return schema;
        }

        if (throwIfNotFound) {
            throw new SchemaException.NotFound(name);
        } else {
            return null;
        }
    }

    public synchronized Map<String, ISchema> getSchemas() {
        if (!this.isRunning()) {
            // Make sure when this method is called, it's initialized, and schemas have been loaded,
            // We don't change the Phase of this object because change of Phase still has implicit dependency,
            // Dependencies have to carefully define the order of phase.
            // So, manually starting this object is much reasonable
            this.start();
        }

        return new TreeMap<>(schemas);
    }

    public void addListener(ISchemaChangedListener listener) {
        listeners.add(listener);

        this.schemas.forEach((name, schema) -> listener.onSchemaChanged(null, schema));
    }

    private void incrementalLoadSchemas() {
        long now = TimeSpan.now().toSeconds() * 1000;

        List<ISchema> changedSchemaList = schemaStorage.getSchemas(this.lastLoadAt);
        log.info("{} schema(s) have been changed since {}.", changedSchemaList.size(), DateTime.toYYYYMMDDhhmmss(this.lastLoadAt));

        for (ISchema changedSchema : changedSchemaList) {
            ISchema oldSchema = this.put(changedSchema.getName(), changedSchema);
            this.onChange(oldSchema, changedSchema);
        }

        this.lastLoadAt = now;
    }

    /**
     * for better debugging only
     */
    private ISchema put(String name, ISchema schema) {
        return this.schemas.put(name, schema);
    }

    private void onChange(ISchema oldSchema, ISchema newSchema) {
        // Copy to list first to avoid a concurrency problem
        ISchemaChangedListener[] listenerList = this.listeners.toArray(new ISchemaChangedListener[0]);

        for (ISchemaChangedListener listener : listenerList) {
            try {
                listener.onSchemaChanged(oldSchema, newSchema);
            } catch (Exception e) {
                log.error("notify onAdd exception", e);
            }
        }
    }

    @Override
    public void start() {
        log.info("Starting schema incremental loader...");

        // Load schemas first.
        incrementalLoadSchemas();

        // start periodic loader
        loaderScheduler = ScheduledExecutorServiceFactor.newSingleThreadScheduledExecutor(NamedThreadFactory.nonDaemonThreadFactory("schema-loader"));
        loaderScheduler.scheduleWithFixedDelay(this::incrementalLoadSchemas,
                                               // no delay to execute the first task
                                               1,
                                               1,
                                               TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        if (loaderScheduler != null) {
            log.info("Shutting down Schema Manager...");
            loaderScheduler.shutdownNow();
            try {
                //noinspection ResultOfMethodCallIgnored
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

    public interface ISchemaChangedListener {
        void onSchemaChanged(ISchema oldSchema, ISchema newSchema);
    }
}
