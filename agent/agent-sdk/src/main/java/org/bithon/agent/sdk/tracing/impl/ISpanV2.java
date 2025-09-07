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

package org.bithon.agent.sdk.tracing.impl;


import org.bithon.agent.sdk.tracing.ISpan;

/**
 * @author frank.chen021@outlook.com
 * @date 7/9/25 6:24 pm
 */
public interface ISpanV2 {
    /**
     * Set current thread info {thread.name, thread.id} to this span
     */
    default ISpan setThreadToTags() {
        return setThreadToTags(Thread.currentThread());
    }

    ISpan setThreadToTags(Thread thread);
}
