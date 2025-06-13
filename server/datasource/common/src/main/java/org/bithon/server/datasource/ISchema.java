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

package org.bithon.server.datasource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.bithon.server.commons.time.Period;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.store.IDataStoreSpec;

import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = DefaultSchema.class)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "internal", value = DefaultSchema.class)
})
public interface ISchema {
    String getName();

    String getDisplayText();

    TimestampSpec getTimestampSpec();

    IColumn getColumnByName(String name);
    Collection<IColumn> getColumns();

    JsonNode getInputSourceSpec();

    IDataStoreSpec getDataStoreSpec();

    ISchema withDataStore(IDataStoreSpec spec);

    void setSignature(String signature);

    @JsonIgnore
    String getSignature();

    /**
     * Whether this schema is created by bithon internally by APIs.
     * In most cases, it should be 'false'
     */
    @JsonIgnore
    default boolean isVirtual() {
        return false;
    }

    Period getTtl();
}
