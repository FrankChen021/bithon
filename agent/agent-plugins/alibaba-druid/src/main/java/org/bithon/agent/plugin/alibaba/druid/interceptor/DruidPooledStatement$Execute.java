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

package org.bithon.agent.plugin.alibaba.druid.interceptor;

import org.bithon.agent.bootstrap.aop.AopContext;

/**
 * @author frankchen
 * @date 2022-07-27
 */
public class DruidPooledStatement$Execute extends DruidStatementAbstractExecute {

    @Override
    protected String getExecutingSql(AopContext aopContext) {
        Object[] args = aopContext.getArgs();
        if (args != null && args.length > 0) {
            return args[0].toString();
        }

        // TODO: executeBatch has no argument, the sql should be retrieved in another way
        return null;
    }
}
