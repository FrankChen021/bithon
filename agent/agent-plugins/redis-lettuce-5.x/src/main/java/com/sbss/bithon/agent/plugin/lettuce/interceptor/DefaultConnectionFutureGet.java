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

package com.sbss.bithon.agent.plugin.lettuce.interceptor;


import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.IBithonObject;

/**
 * @author frankchen
 */
public class DefaultConnectionFutureGet extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Object result = aopContext.getReturning();
        if (result instanceof IBithonObject && aopContext.getTarget() instanceof IBithonObject) {
            ((IBithonObject) result).setInjectedObject(((IBithonObject) aopContext.getTarget()).getInjectedObject());
        }
    }
}
