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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bithon.agent.observability.tracing.context.ITraceSpan;

import java.util.concurrent.ForkJoinPool;


/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/24 17:01
 */
public class ForkJoinTaskContext {
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public String className;

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public String method;

    // These fields are set by interceptors in other modules (thread-jdk8, thread-jdk9, thread-jdk21)
    // SpotBugs only sees them being set to null in this module, hence the suppression
    @SuppressFBWarnings("UWF_NULL_FIELD")
    public ITraceSpan rootSpan;

    @SuppressFBWarnings("UWF_NULL_FIELD")
    public ForkJoinPool pool;

    public ForkJoinTaskContext(String className, String method) {
        this.className = className;
        this.method = method;
    }
}
