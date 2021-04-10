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

package com.sbss.bithon.agent.core.tracing.context;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 11:18 下午
 */
public class TraceContextHolder {
    private static final ThreadLocal<TraceContext> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ASYNC_HOLDER = new ThreadLocal<>();

    public static void set(TraceContext tracer) {
        HOLDER.set(tracer);
    }

    public static void remove() {
        HOLDER.set(null);
    }

    public static TraceContext get() {
        return HOLDER.get();
    }

    public static Boolean getAsy() {
        return ASYNC_HOLDER.get();
    }

    public static void setAsy(Boolean asy) {
        ASYNC_HOLDER.set(asy);
    }
}
