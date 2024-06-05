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

package org.bithon.component.commons.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author frankchen
 */
public class ReflectionUtils {

    public static Object getFieldValue(Object obj,
                                       String fieldName) {
        if (obj == null) {
            return null;
        }

        Object result = null;
        Field field;
        try {
            field = getField(obj.getClass(), fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
        field.setAccessible(true);
        try {
            result = field.get(obj);
        } catch (IllegalAccessException ignored) {
        }
        return result;
    }

    public static Field getField(Class<?> clazz,
                                 String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    public static Map<String, Object> getFields(Object object) {
        return getFields(object, new HashMap<>(16));
    }

    public static Map<String, Object> getFields(Object object, Map<String, Object> map) {
        if (null != object) {
            Field[] declaredFields = object.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                String fieldName = field.getName();
                if (fieldName.endsWith("_")) {
                    // protobuf generated message classes
                    fieldName = fieldName.substring(0, fieldName.length() - 1);
                }

                String methodName = "get" + fieldName.substring(0, 1).toUpperCase(Locale.ENGLISH) + fieldName.substring(1);
                Method method = null;
                try {
                    method = object.getClass().getMethod(methodName);
                    method.setAccessible(true);
                    Object value = method.invoke(object);
                    map.put(fieldName, value);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                }
            }
        }
        return map;
    }

    public static void setFieldValue(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field f = getField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
