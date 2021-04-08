package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:34 下午
 */
public class NumberUtils {
    public static long getLong(Object value) {
        if ( value == null ) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("Unknown type of value for Long: " + value.getClass().getName());
    }

    public static double getDouble(Object value) {
        if ( value == null ) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new IllegalArgumentException("Unknown type of value for Double: " + value.getClass().getName());
    }
}
