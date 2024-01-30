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
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.query.IDataSourceReader;

import java.io.IOException;

/**
 * @author Frank Chen
 * @date 18/5/23 1:59 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = MetricDataSourceSpec.class)
@JsonSubTypes(value = {
    // For backward compatibility
    @JsonSubTypes.Type(name = "internal", value = MetricDataSourceSpec.class),

    @JsonSubTypes.Type(name = "metric", value = MetricDataSourceSpec.class),
})
@Experimental
public interface IDataStoreSpec {

    /**
     * the name of data source at store layer
     */
    String getStore();

    void setSchema(ISchema schema);

    @JsonIgnore
    boolean isInternal();

    default IDataStoreSpec hideSensitiveInformation() {
        return this;
    }

    IDataSourceReader createReader() throws IOException;
}
