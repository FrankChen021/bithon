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

package org.bithon.server.storage.meta;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.storage.datasource.DataSourceSchema;

import java.io.IOException;
import java.util.List;

/**
 * @author Frank Chen
 * @date 7/1/22 1:39 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ISchemaStorage {

    /**
     * get changed schemas after given timestamp
     */
    List<DataSourceSchema> getSchemas(long afterTimestamp);

    /**
     * get all schemas
     */
    List<DataSourceSchema> getSchemas();

    DataSourceSchema getSchemaByName(String name);

    void update(String name, DataSourceSchema schema) throws IOException;
    void putIfNotExist(String name, DataSourceSchema schema) throws IOException;

    default void initialize() {
    }
}
