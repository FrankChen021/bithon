package com.sbss.bithon.agent.core.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A context which shares some data among interceptors in different plugins
 *
 * @author frankchen
 * @date 2020-12-31 22:18:53
 */
public class InterceptorContext {
    public static final String KEY_URI = "uri";
    public static final String KEY_TRACEID = "traceId";

    private static final ThreadLocal<Map<String, Object>> HOLDER = ThreadLocal.withInitial(() -> new ConcurrentHashMap<>(17));

    public static void set(String key, Object obj) {
        HOLDER.get().put(key, obj);
    }

    public static Object get(String key) {
        return HOLDER.get().get(key);
    }

    public static <T> T getAs(String key) {
        return (T) HOLDER.get().get(key);
    }

    public static void remove(String key) {
        HOLDER.get().remove(key);
    }
}
