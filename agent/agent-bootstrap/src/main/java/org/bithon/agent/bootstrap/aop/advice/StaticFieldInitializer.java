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

package org.bithon.agent.bootstrap.aop.advice;

import org.bithon.agent.bootstrap.aop.BootstrapHelper;
import shaded.net.bytebuddy.implementation.LoadedTypeInitializer;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * @author Frank Chen
 * @date 19/2/22 3:47 PM
 */
public class StaticFieldInitializer implements LoadedTypeInitializer {
    private final String fieldName;
    private final Object value;

    public StaticFieldInitializer(String fieldName, Object value) {
        this.fieldName = fieldName;
        this.value = value;
    }

    @Override
    public void onLoad(Class<?> type) {
        try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception e) {
            BootstrapHelper.createAopLogger(StaticFieldInitializer.class)
                           .error(String.format(Locale.ENGLISH,
                                                "Failed to inject interceptor[%s] due to %s",
                                                value.getClass().getName(),
                                                e.getMessage()),
                                  e);
        }
    }

    @Override
    public boolean isAlive() {
        return true;
    }
}
