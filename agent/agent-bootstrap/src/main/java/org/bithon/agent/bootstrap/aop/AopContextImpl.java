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

package org.bithon.agent.bootstrap.aop;

import java.lang.reflect.Executable;

/**
 * @author Frank Chen
 * @date 28/12/22 10:29 am
 */
public class AopContextImpl extends AopContext {

    public AopContextImpl(Executable method,
                          Object target,
                          Object[] args) {
        super(method, target, args);
    }

    public void setException(Throwable throwable) {
        this.exception = throwable;
    }

    /**
     * An internal interface. SHOULD NOT be used by users' code
     */
    public void onBeforeTargetMethodInvocation() {
        startNanoTime = System.nanoTime();
        startTimestamp = System.currentTimeMillis();
    }

    /**
     * An internal interface. SHOULD NOT be used by users' code
     */
    public void onAfterTargetMethodInvocation() {
        endNanoTime = System.nanoTime();
        endTimestamp = System.currentTimeMillis();
    }
}
