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

import java.util.function.Supplier;

/**
 * Support of {@link java.util.concurrent.CompletableFuture#supplyAsync(Supplier)} to propagate the task context
 *
 * @author frank.chen021@outlook.com
 * @date 27/3/25 12:13 am
 */
public class CompletableFuture$AsyncSupply$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) throws Exception {
        Supplier<?> supplier = aopContext.getArgAs(1);

        IBithonObject task = aopContext.getTargetAs();
        task.setInjectedObject(new ForkJoinTaskContext(supplier.getClass().getName(), "run"));
    }
}
