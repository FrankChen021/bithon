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

    private static final ThreadLocal<Map<String, Object>> HOLDER = ThreadLocal.withInitial(() -> new ConcurrentHashMap<>(
        17));

    public static void set(String key, Object obj) {
        HOLDER.get().put(key, obj);
    }

    public static Object get(String key) {
        return HOLDER.get().get(key);
    }

    public static <T> T getAs(String key) {
        //noinspection unchecked
        return (T) HOLDER.get().get(key);
    }

    public static void remove(String key) {
        try {
            HOLDER.get().remove(key);
        } catch (Exception ignored) {
        }
    }
}
