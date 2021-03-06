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

package org.bithon.agent.core.tracing.context;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 11:18 下午
 */
public class TraceContextHolder {
    private static final ThreadLocal<ITraceContext> HOLDER = new ThreadLocal<>();

    public static void set(ITraceContext tracer) {
        HOLDER.set(tracer);
    }

    public static void remove() {
        HOLDER.remove();
    }

    public static ITraceContext current() {
        return HOLDER.get();
    }
}
