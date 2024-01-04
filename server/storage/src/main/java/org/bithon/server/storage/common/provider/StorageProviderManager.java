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

package org.bithon.server.storage.common.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.InvalidConfigurationException;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/27 14:34
 */
@Service
public class StorageProviderManager {

    static class ScopedObjectMapper implements Closeable {
        private final ObjectMapper delegation;
        private final InjectableValues old;

        public ScopedObjectMapper(ObjectMapper objectMapper, InjectableValues values) {
            this.delegation = objectMapper;
            old = this.delegation.getInjectableValues();
            this.delegation.setInjectableValues(new InjectableValues() {
                @Override
                public Object findInjectableValue(Object valueId, DeserializationContext ctx, BeanProperty forProperty, Object beanInstance) throws JsonMappingException {
                    Object obj = values.findInjectableValue(valueId, ctx, forProperty, beanInstance);
                    if (obj != null) {
                        return obj;
                    }
                    return old.findInjectableValue(valueId, ctx, forProperty, beanInstance);
                }
            });
        }

        public <T> T readValue(String text, Class<T> clazz) throws JsonProcessingException {
            return this.delegation.readValue(text, clazz);
        }

        @Override
        public void close() throws IOException {
            this.delegation.setInjectableValues(this.old);
        }
    }

    static class StorageConfigurationInjector extends InjectableValues {
        private final IStorageConfiguration configuration;

        StorageConfigurationInjector(IStorageConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Object findInjectableValue(Object valueId, DeserializationContext ctx, BeanProperty forProperty, Object beanInstance) {
            Class<?> targetClass = forProperty.getType().getRawClass();
            if (configuration.getClass().isAssignableFrom(targetClass)) {
                return configuration;
            }
            return null;
        }
    }

    /**
     * JSON style configuration from the YAML
     */
    private final StorageProviderConfigs configs;

    /**
     * Objects that are created from {@link #configs}
     */
    private final Map<String, IStorageConfiguration> configurations;
    private final ObjectMapper objectMapper;

    public StorageProviderManager(StorageProviderConfigs configs, ObjectMapper objectMapper) {
        this.configs = configs;
        this.configurations = new HashMap<>(7);
        this.objectMapper = objectMapper;
    }

    public IStorageConfiguration computeIfAbsent(String name) {
        return this.configurations.computeIfAbsent(name, (k) -> {
            IStorageConfiguration provider = this.configurations.get(name);
            if (provider != null) {
                return provider;
            }

            // Initialize the provider
            Map<String, Object> config = configs.getProviders().get(name);
            InvalidConfigurationException.throwIf(config == null, StringUtils.format("Storage [%s] not provided in the configurations.", name));

            String type = (String) config.get("type");
            if (StringUtils.isEmpty(type)) {
                // Use the 'name' property which can simplify the configuration
                type = name;
            }

            Map<String, Object> obj = new HashMap<>();
            obj.put("type", type);
            obj.put("props", config);

            try {
                String text = objectMapper.writeValueAsString(obj);
                return objectMapper.readValue(text, IStorageConfiguration.class);
            } catch (InvalidTypeIdException e) {
                throw new InvalidConfigurationException(StringUtils.format("Storage [%s] is not supported.", type));
            } catch (IOException e) {
                throw new InvalidConfigurationException(StringUtils.format("Unable to create storage [%s]: %s", type, e.getMessage()),
                                                        e);
            }
        });
    }

    public <T> T createStorage(String providerName, Class<T> clazz) throws IOException {
        IStorageConfiguration configuration = computeIfAbsent(providerName);

        try (ScopedObjectMapper om = new ScopedObjectMapper(this.objectMapper, new StorageConfigurationInjector(configuration))) {
            String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", configuration.getType());
            return om.readValue(jsonType, clazz);
        }
    }
}
