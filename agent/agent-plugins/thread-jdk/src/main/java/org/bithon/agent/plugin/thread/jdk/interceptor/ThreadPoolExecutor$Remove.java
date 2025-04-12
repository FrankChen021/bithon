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

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.plugin.thread.jdk.utils.ObservedTask;

/**
 * {@link java.util.concurrent.ThreadPoolExecutor#remove(Runnable)}
 *
 * @author frank.chen021@outlook.com
 * @date 2023/7/3 21:37
 */
public class ThreadPoolExecutor$Remove extends BeforeInterceptor {
    /**
     * When calling the {@link java.util.concurrent.ThreadPoolExecutor#remove(Runnable)},
     * the implementation uses this pattern to compare the runnable object
     * to determine if the current object from the queue is the right object to delete:
     * <p>
     * userRunnableObject.equals(objectFromQueue)
     * <p>
     * So, we need to turn the userRunnableObject as an object wrapped by {@link ObservedTask},
     * In such a case, the above statement would be:
     * <p>
     * wrappedRunnableObject.equals(objectFromQueue).
     * <p>
     * Since we have overridden the {@link ObservedTask#equals(Object)} to compare the inner Runnable object,
     * above equals function can return the correct result if two inner Runnable objects are the same.
     */
    @Override
    public void before(AopContext aopContext) {
        aopContext.getArgs()[0] = new ObservedTask(aopContext.getTargetAs(), aopContext.getArgAs(0), null);
    }
}
