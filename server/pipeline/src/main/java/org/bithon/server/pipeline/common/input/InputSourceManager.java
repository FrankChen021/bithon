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

package org.bithon.server.pipeline.common.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.IDataSource;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/16 21:35
 */
@Slf4j
public class InputSourceManager implements DataSourceSchemaManager.IDataSourceSchemaListener {

    private final Map<String, IInputSource> inputSources = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public InputSourceManager(DataSourceSchemaManager dataSourceSchemaManager, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        dataSourceSchemaManager.addListener(this);
    }

    @Override
    public void onChange(IDataSource oldSchema, IDataSource newSchema) {
        if (oldSchema != null
            && Objects.equals(oldSchema.getSignature(), newSchema.getSignature())) {
            // same signature, do nothing
            return;
        }

        // stop input
        if (oldSchema != null && oldSchema.getInputSourceSpec() != null) {
            log.info("Stop input source for schema [{}]", oldSchema.getName());
            IInputSource inputSource = inputSources.remove(oldSchema.getSignature());
            if (inputSource != null) {
                inputSource.stop();
            }
        }

        // start for the new schema
        if (newSchema.getInputSourceSpec() != null && !NullNode.getInstance().equals(newSchema.getInputSourceSpec())) {
            log.info("Start input source for schema [{}]", newSchema.getName());

            IInputSource inputSource = objectMapper.convertValue(newSchema.getInputSourceSpec(), IInputSource.class);
            inputSource.start(newSchema);
            inputSources.put(newSchema.getSignature(), inputSource);
        }
    }
}
