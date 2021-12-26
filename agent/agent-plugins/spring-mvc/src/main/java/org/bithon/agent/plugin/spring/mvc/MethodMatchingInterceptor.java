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

package org.bithon.agent.plugin.spring.mvc;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.utils.CollectionUtils;
import org.bithon.component.logging.ILogAdaptor;
import org.bithon.component.logging.LoggerFactory;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.util.Set;

/**
 * frank.chen021@outlook.com
 */
public class MethodMatchingInterceptor extends AbstractInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(MethodMatchingInterceptor.class);

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Object mapping = aopContext.getArgs()[2];
        if (!(mapping instanceof RequestMappingInfo)) {
            log.warn("spring mvc registering mapping pattern with unrecognized class");
            return;
        }

        RequestMappingInfo mappingInfo = (RequestMappingInfo) mapping;
        Set<String> patterns = mappingInfo.getPatternsCondition().getPatterns();
        if (CollectionUtils.isEmpty(patterns)) {
            return;
        }

        //TODO: keep patterns in temp storage
        // and send the patterns after detectHandlerMethods(final Object handler) has been intercepted
        /*
        EventMessage eventMessage = new EventMessage();
        Dispatcher eventDispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVNETS);
        eventDispatcher.sendMessage(eventMessage);
         */
    }
}
