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

package org.bithon.agent.plugin.thread.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.plugin.thread.utils.ObservedTask;

/**
 * {@link java.util.concurrent.ThreadPoolExecutor#remove(Runnable)}
 *
 * @author frank.chen021@outlook.com
 * @date 2023/7/3 21:37
 */
public class ThreadPoolExecutor$Remove extends BeforeInterceptor {
    /**
     * The {@link java.util.concurrent.ThreadPoolExecutor#remove(Runnable)} removes the object from the queue,
     * however, in the queue, we store the object of {@link ObservedTask}, so we need to change the input args as {@link ObservedTask}.
     * <p>
     * The {@link ObservedTask} already overrides its {@link ObservedTask#equals(Object)}
     * and {@link ObservedTask#compareTo(Object)} to handle the comparison correctly.
     */
    @Override
    public void before(AopContext aopContext) {
        aopContext.getArgs()[0] = new ObservedTask(aopContext.getTargetAs(), aopContext.getArgAs(0), null);
    }
}
