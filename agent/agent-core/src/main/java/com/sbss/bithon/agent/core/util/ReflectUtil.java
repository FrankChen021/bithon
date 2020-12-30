package com.sbss.bithon.agent.core.util;

import java.lang.reflect.Field;

public class ReflectUtil {

    /**
     * @param obj
     * @param fieldName
     * @return 运行返回null
     */
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

    /**
     * 该方法是递归，当fieldName取值为null，有NPE
     *
     * @param clazz
     * @param fieldName
     * @return
     */
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
