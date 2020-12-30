package com.sbss.bithon.agent.core.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by shiweilu on 2018/7/17.
 */
public class ContextHolder {
    private static ThreadLocal<Map<String, Object>> holder = new ThreadLocal<Map<String, Object>>() {
        protected Map<String, Object> initialValue() {
            return new ConcurrentHashMap<String, Object>();
        }
    };

    public static void set(String key,
                           Object obj) {
        holder.get().put(key, obj);
    }

    public static Object get(String key) {
        return holder.get().get(key);
    }

    public static void remove(String key) {
        holder.get().remove(key);
    }
}
