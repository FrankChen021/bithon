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

package org.bithon.agent.plugin.xxl.job.interceptor;

/**
 * @author Frank Chen
 * @date 26/2/24 2:49 pm
 */
public class UserDefinedContext {
    private static final ThreadLocal<UserDefinedContext> HOLDER = new ThreadLocal<>();

    private final String traceId;
    private final String parentSpanId;

    public UserDefinedContext(String traceId, String parentSpanId) {
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
    }


    public String getTraceId() {
        return traceId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public static UserDefinedContext getAndRemove() {
        UserDefinedContext ctx = HOLDER.get();
        if (ctx != null) {
            HOLDER.remove();
        }
        return ctx;
    }

    public static void set(String traceId, String parentSpanId) {
        HOLDER.set(new UserDefinedContext(traceId, parentSpanId));
    }

    public static void remove() {
        HOLDER.remove();
    }
}
