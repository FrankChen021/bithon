package org.bithon.server.metric.storage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.metric.DataSourceSchema;

import java.io.IOException;
import java.util.List;

/**
 * @author Frank Chen
 * @date 7/1/22 1:39 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ISchemaStorage {

    List<DataSourceSchema> getSchemas();

    DataSourceSchema getSchemaByName(String name);

    void update(String name, DataSourceSchema schema) throws IOException;
    void putIfNotExist(String name, DataSourceSchema schema) throws IOException;

    default void initialize() {
    }
}
