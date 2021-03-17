package com.sbss.bithon.server.metric;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
public class DataSourceSchemaManager {
    private final List<IDataSourceSchemaListener> listeners = new ArrayList<>();
    private final Map<String, DataSourceSchema> schemas = new ConcurrentHashMap<>();

    public void addDataSourceSchema(DataSourceSchema schema) {
        schemas.put(schema.getName(), schema);

        for (IDataSourceSchemaListener listener : listeners) {
            try {
                listener.onAdd(schema);
            } catch (Exception e) {
                log.error("notify onAdd exception", e);
            }
        }
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

        try (InputStream is = this.getClass().getClassLoader()
                                  .getResourceAsStream(String.format("schema/%s.json", name))) {

            ObjectMapper om = new ObjectMapper();
            DataSourceSchema dataSourceSchema = om.readValue(is, DataSourceSchema.class);
            addDataSourceSchema(dataSourceSchema);
            return dataSourceSchema;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, DataSourceSchema> getDataSources() {
        return new HashMap<>(schemas);
    }

    public void addListener(IDataSourceSchemaListener listener) {
        listeners.add(listener);
    }

    public interface IDataSourceSchemaListener {
        void onRmv(DataSourceSchema dataSourceSchema);

        void onAdd(DataSourceSchema dataSourceSchema);
    }
}
