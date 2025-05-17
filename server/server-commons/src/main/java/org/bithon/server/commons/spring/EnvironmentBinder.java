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

package org.bithon.server.commons.spring;


import org.bithon.component.commons.utils.HumanReadableNumber;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 16/5/25 10:52 pm
 */
@Component
public class EnvironmentBinder {
    private final Binder binder;

    public static EnvironmentBinder from(ConfigurableEnvironment environment) {
        return new EnvironmentBinder(environment);
    }

    public EnvironmentBinder(ConfigurableEnvironment environment) {
        ConfigurableConversionService conversionService = environment.getConversionService();

        try {
            // The ConversionService does not provide an API to check if a converter has been added
            // Although there's a canConvert method, since HumanReadableNumber an extended Number class, there's a built-in Strong to Number converter,
            // This will always return true.
            // So we have to convert to detect if the convert has been added
            //
            conversionService.convert("1KiB", HumanReadableNumber.class);
        } catch (ConversionFailedException ignored) {
            conversionService.addConverter(String.class, HumanReadableNumber.class, HumanReadableNumber::of);
        }

        this.binder = new Binder(ConfigurationPropertySources.get(environment),
                                 new PropertySourcesPlaceholdersResolver(environment),
                                 conversionService);
    }

    /**
     * Bind the property with the given name to the specified type.
     *
     * @param name the name of the property
     * @param type the type to bind to
     * @param <T>  the type of the property
     * @return the bound value, or null if not found
     */
    public <T> T bind(String name, Class<T> type) {
        return binder.bind(name, type).orElse(null);
    }

    public <T> T bind(String name, Class<T> type, Supplier<T> defaultSupplier) {
        return binder.bind(name, type).orElseGet(defaultSupplier);
    }
}
