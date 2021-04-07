package com.sbss.bithon.server.webapp.utils;

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
            try {
                Field[] declaredFields = object.getClass().getFields();
                for (Field field : declaredFields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    String fieldName = field.getName();
                    String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    Method method = object.getClass().getMethod(methodName);
                    Object value = method.invoke(object);

                    map.put(fieldName, value);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return map;
    }
}
