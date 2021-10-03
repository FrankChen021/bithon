/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.bootstrap.aop;

/**
 * <pre>
 *     NOTE: subclass of this class MUST be declared as PUBLIC
 * </pre>
 *
 * @author frankchen
 * @date 2020-12-31 22:20:11
 */
public abstract class AbstractInterceptor {

    /**
     * @return false if this interceptor should not be loaded
     */
    public boolean initialize() throws Exception {
        return true;
    }

    public void onConstruct(AopContext aopContext) throws Exception {
    }

    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        return InterceptionDecision.CONTINUE;
    }

    /**
     * Called after execution of target intercepted method
     * If {@link #onMethodEnter} returns {@link InterceptionDecision#SKIP_LEAVE}, call of this method will be skipped
     */
    public void onMethodLeave(AopContext aopContext) throws Exception {
    }

    /**
     * Replacement interceptor
     */
    public Object onExecute(Object[] args) {
        return null;
    }
}
