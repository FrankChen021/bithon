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

package org.bithon.server.datasource.vm;


import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.store.ExternalDataStoreSpec;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 28/4/25 9:34 pm
 */
public class VMDataStoreSpec extends ExternalDataStoreSpec {

    private final String url;
    private final ApplicationContext applicationContext;

    public VMDataStoreSpec(@JsonProperty("properties") Map<String, Object> properties,
                           @JsonProperty("store") String store,
                           @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        super(properties, store);
        this.url = (String) Preconditions.checkNotNull(properties.get("url"), "url");
        this.applicationContext = applicationContext;
    }

    @Override
    public IDataStoreSpec hideSensitiveInformation() {
        return super.hideSensitiveInformation();
    }

    @Override
    public IDataSourceReader createReader() {
        return new VMDataSourceReader(url, applicationContext);
    }
}
