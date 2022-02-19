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

/**
 * @author Frank Chen
 * @date 3/10/21 16:23
 */
public abstract class IReplacementInterceptor extends AbstractInterceptor {

    @Override
    public final void onConstruct(AopContext aopContext) throws Exception {
    }

    @Override
    public final InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        return super.onMethodEnter(aopContext);
    }

    @Override
    public final void onMethodLeave(AopContext aopContext) throws Exception {
    }

    public abstract Object onExecute(Object[] args);
}
