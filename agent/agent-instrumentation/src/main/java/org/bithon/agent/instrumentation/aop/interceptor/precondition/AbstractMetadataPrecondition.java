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

package org.bithon.agent.instrumentation.aop.interceptor.precondition;

import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/6 23:58
 */
public abstract class AbstractMetadataPrecondition implements IInterceptorPrecondition {
    protected final String metaFileName;
    protected final String propertyName;
    protected final String propertyValue;

    public AbstractMetadataPrecondition(String metaFileName,
                                        String propertyName,
                                        String propertyValue) {
        this.metaFileName = metaFileName;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    @Override
    public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
        try (InputStream inputStream = classLoader.getResourceAsStream(metaFileName)) {
            if (inputStream == null) {
                return false;
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            String value = properties.getProperty(propertyName);
            return matches(this.propertyValue, value);
        } catch (IOException ignored) {
        }
        return false;
    }

    protected abstract boolean matches(String expected, String actual);
}
