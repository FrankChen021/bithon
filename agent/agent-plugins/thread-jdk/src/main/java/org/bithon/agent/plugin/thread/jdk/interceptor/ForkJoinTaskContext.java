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

package org.bithon.agent.plugin.thread.jdk.interceptor;

import org.bithon.agent.observability.tracing.context.ITraceSpan;

import java.util.concurrent.ForkJoinPool;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/24 17:01
 */
public class ForkJoinTaskContext {
    public String className;
    public String method;
    public ITraceSpan rootSpan;
    public ForkJoinPool pool;

    public ForkJoinTaskContext(String className, String method) {
        this.className = className;
        this.method = method;
    }
}
