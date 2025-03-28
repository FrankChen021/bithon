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

package org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;

/**
 * {@link com.alibaba.druid.pool.DruidPooledConnection#close()}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 21:52
 */
public class DruidPooledConnection$Close extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        return super.before(aopContext);
    }

    @Override
    public void after(AopContext aopContext) throws Exception {
        super.after(aopContext);
    }
}
