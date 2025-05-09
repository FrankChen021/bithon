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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.datasource.ISchema;

import java.io.IOException;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 7/1/22 1:39 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ISchemaStorage {

    /**
     * get changed schemas after given timestamp
     */
    List<ISchema> getSchemas(long afterTimestamp);

    boolean containsSchema(String name);

    /**
     * get all schemas
     */
    List<ISchema> getSchemas();

    ISchema getSchemaByName(String name);

    void update(String name, ISchema schema) throws IOException;
    void putIfNotExist(String name, ISchema schema) throws IOException;
    void putIfNotExist(String name, String schema);

    default void initialize() {
    }
}
