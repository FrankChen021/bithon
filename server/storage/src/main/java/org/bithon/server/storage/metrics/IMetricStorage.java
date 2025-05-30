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

package org.bithon.server.storage.metrics;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.storage.common.IStorage;
import org.bithon.server.storage.common.expiration.IExpirable;

import java.io.IOException;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/1 4:53 下午
 * <p>
 * use ObjectMapper.registerSubTypes to register a type of subclass for deserialization
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IMetricStorage extends IStorage, IExpirable {

    default String getName() {
        return "metrics";
    }

    IMetricWriter createMetricWriter(ISchema schema) throws IOException;

    IDataSourceReader createMetricReader(ISchema schema);

    List<String> getBaselineDates();

    void saveBaseline(String date, int keepDays);

    default void initialize() {
    }
}
