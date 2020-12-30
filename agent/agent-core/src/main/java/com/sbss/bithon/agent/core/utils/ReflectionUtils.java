package com.sbss.bithon.agent.core.utils;

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
