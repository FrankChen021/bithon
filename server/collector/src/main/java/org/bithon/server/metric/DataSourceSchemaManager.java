/*
 *    Copyright 2020 bithon.cn
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 * @date 2020-08-21 15:13:41
 */
@Slf4j
@Service
public class DataSourceSchemaManager implements SmartLifecycle {
    private final List<IDataSourceSchemaListener> listeners = new ArrayList<>();
    private final Map<String, DataSourceSchema> schemas = new ConcurrentHashMap<>();

    public boolean addDataSourceSchema(DataSourceSchema schema) {
        if (schemas.putIfAbsent(schema.getName(), schema) == null) {
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

    public DataSourceSchema getDataSourceSchema(String name) {
        DataSourceSchema schema = schemas.get(name);
        if (schema != null) {
            return schema;
        }
        synchronized (this) {
            try (InputStream is = this.getClass().getClassLoader()
                                      .getResourceAsStream(String.format("schema/%s.json", name))) {
                if (is != null) {
                    ObjectMapper om = new ObjectMapper();
                    om.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
                    DataSourceSchema dataSourceSchema = om.readValue(is, DataSourceSchema.class);
                    addDataSourceSchema(dataSourceSchema);
                    return dataSourceSchema;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new DataSourceNotFoundException("Can't find schema for datasource " + name);
    }

    public Map<String, DataSourceSchema> getDataSources() {
        return new HashMap<>(schemas);
    }

    public void addListener(IDataSourceSchemaListener listener) {
        listeners.add(listener);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
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
