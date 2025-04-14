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

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;

/**
 * Hook on {@link java.util.concurrent.ForkJoinTask.AdaptedRunnableAction#AdaptedRunnableAction(Runnable)}
 * <p>
 * We need to record the wrapped runnable in the spans,
 * however, from JDK 11,
 * we're NOT able to get the runnable object from the ForkJoinTask object without adding --add-exports directive.
 * <p>
 * In order NOT to add that directive at user's application side, we have to get store the information by ourselves.
 * And the stored information will be read in {@link ForkJoinPool$ExternalPush}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/24 16:44
 */
public class ForkJoinTaskAdaptedRunnableAction$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        Runnable runnable = aopContext.getArgAs(0);

        IBithonObject task = aopContext.getTargetAs();
        task.setInjectedObject(new ForkJoinTaskContext(runnable.getClass().getName(), "run"));
    }
}
