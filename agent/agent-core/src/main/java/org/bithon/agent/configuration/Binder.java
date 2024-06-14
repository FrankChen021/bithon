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

package org.bithon.agent.configuration;

import org.bithon.agent.configuration.validation.Validator;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.NullNode;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * @author Frank Chen
 * @date 21/2/24 10:14 am
 */
public class Binder {

    public static <T> T bind(String propertyPath, JsonNode configuration, Class<T> clazz) {
        return bind(propertyPath,
                    configuration,
                    clazz,
                    () -> {
                        // default value provider
                        try {
                            if (clazz == Boolean.class) {
                                //noinspection unchecked
                                return (T) Boolean.FALSE;
                            }
                            if (clazz.isArray()) {
                                //noinspection unchecked
                                return (T) Array.newInstance(clazz.getComponentType(), 0);
                            }
                            Constructor<T> ctor = clazz.getDeclaredConstructor();
                            ctor.setAccessible(true);
                            return ctor.newInstance();
                        } catch (IllegalAccessException e) {
                            throw new AgentException("Unable create instance for [%s]: %s", clazz.getName(), e.getMessage());
                        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                            throw new AgentException("Unable create instance for [%s]: %s",
                                                     clazz.getName(),
                                                     e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
                        }
                    });
    }

    public static <T> T bind(String propertyPath, JsonNode configuration, Class<T> clazz, Supplier<T> defaultSupplier) {
        if (configuration == null || configuration instanceof NullNode) {
            // Don't check if the configuration.isEmpty()
            // because the node here might be mapped to a primitive or simple data type
            return defaultSupplier.get();
        }

        T value;
        try {
            value = ObjectMapperConfigurer.configure(new ObjectMapper())
                                          .convertValue(configuration, clazz);
        } catch (IllegalArgumentException e) {
            throw new AgentException(e,
                                     "Unable to read type of [%s] from configuration: %s",
                                     clazz.getSimpleName(),
                                     e.getMessage());
        }

        String violation = Validator.validate(propertyPath, value);
        if (violation != null) {
            throw new AgentException("Invalid configuration for type of [%s]: %s",
                                     clazz.getSimpleName(),
                                     violation);
        }

        return value;
    }
}
