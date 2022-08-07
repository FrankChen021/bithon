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

package org.bithon.agent.plugin.spring.webflux.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.reactivestreams.Publisher;

/**
 * @author frank.chen021@outlook.com
 * @date 29/11/21 3:35 pm
 */
public class Flux$Timeout extends AbstractInterceptor {

    /**
     * replace the timeout callback so that we have the opportunity to do sth when timeout
     *
     * {@link HttpClientFinalizer$ResponseConnection#onMethodLeave(AopContext)} will inject a timeout callback
     */
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Runnable timeoutCallback = aopContext.castInjectedOnTargetAs();
        if (timeoutCallback == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        Publisher originalFallback = aopContext.getArgAs(2);

        Publisher newFallback = subscriber -> {
            // should be the first since the fallback.subscribe will trigger other callbacks hooked on it
            timeoutCallback.run();

            if (originalFallback != null) {
                originalFallback.subscribe(subscriber);
            }
        };

        // replace the original fallback
        aopContext.getArgs()[2] = newFallback;
        return InterceptionDecision.SKIP_LEAVE;
    }
}
