/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.core.utils;

import java.lang.reflect.Field;

/**
 * @author frankchen
 */
public class ReflectionUtils {

    public static Object getFieldValue(Object obj,
                                       String fieldName) {
        Object result = null;
        Field field = getTargetField(obj.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        try {
            result = field.get(obj);
        } catch (IllegalAccessException ignored) {
        }
        return result;
    }

    private static Field getTargetField(Class<?> clazz,
                                        String fieldName) {
        Field field = null;
        if (clazz == null) {
            return null;
        }
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {
        }
        if (field == null) {
            field = getTargetField(clazz.getSuperclass(), fieldName);
        }
        return field;
    }
}
