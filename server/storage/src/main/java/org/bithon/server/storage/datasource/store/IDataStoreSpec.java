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

package org.bithon.server.storage.datasource.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.component.commons.Experimental;
import org.bithon.server.storage.datasource.IDataSource;
import org.bithon.server.storage.datasource.query.IDataSourceReader;

import java.io.IOException;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 18/5/23 1:59 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = InternalDataSourceSpec.class)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "metric", value = InternalDataSourceSpec.class),
    @JsonSubTypes.Type(name = "external", value = ExternalDataStoreSpec.class),
})
@Experimental
public interface IDataStoreSpec {

    /**
     * the name of data source at store layer
     */
    String getStore();

    void setDataSourceSchema(IDataSource schema);

    @JsonIgnore
    boolean isInternal();

    Map<String, String> getProperties();

    IDataStoreSpec withProperties(Map<String, String> properties);

    IDataSourceReader createReader() throws IOException;
}
