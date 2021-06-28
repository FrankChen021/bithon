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

package com.sbss.bithon.server.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/12 9:34 下午
 */
public class ReflectionUtils {

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

                String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
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
}
