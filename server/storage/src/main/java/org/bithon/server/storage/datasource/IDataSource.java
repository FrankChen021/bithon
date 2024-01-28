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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.bithon.server.commons.time.Period;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;

/**
 * @author frank.chen021@outlook.com
 */
public interface IDataSource {
    @JsonIgnore
    boolean isVirtual();

    String getName();
    String getDisplayText();

    TimestampSpec getTimestampSpec();
    IColumn getColumnByName(String name);

    JsonNode getInputSourceSpec();

    IDataStoreSpec getDataStoreSpec();
    IDataSource withDataStore(IDataStoreSpec spec);

    void setSignature(String signature);

    @JsonIgnore
    String getSignature();

    Period getTtl();


}
