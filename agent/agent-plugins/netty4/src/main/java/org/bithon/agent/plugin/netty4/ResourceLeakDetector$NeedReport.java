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

package org.bithon.agent.plugin.netty4;

import org.bithon.agent.instrumentation.aop.interceptor.ReplaceInterceptor;

/**
 * Interceptor of {@link io.netty.util.ResourceLeakDetector#needReport()}
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/13 22:47
 */
public class ResourceLeakDetector$NeedReport extends ReplaceInterceptor {
    @Override
    public Object execute(Object[] args, Object returning) {
        // Always return true to make sure the events can be logged
        return true;
    }
}
