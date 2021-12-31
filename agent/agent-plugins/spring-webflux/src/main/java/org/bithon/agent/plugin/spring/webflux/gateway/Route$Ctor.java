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

package org.bithon.agent.plugin.spring.webflux.gateway;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.utils.CollectionUtils;
import org.bithon.agent.plugin.spring.webflux.gateway.aop.GatewayAopDynamicInstaller;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.Route;

import java.util.List;

/**
 * Install handler to user gateway filter
 *
 * @author Frank Chen
 * @date 30/12/21 2:16 PM
 */
public class Route$Ctor extends AbstractInterceptor {

    @Override
    public boolean initialize() {


        return true;
    }

    @Override
    public void onConstruct(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }
        Route route = aopContext.castTargetAs();

        List<GatewayFilter> filters = route.getFilters();
        if (CollectionUtils.isEmpty(filters)) {
            return;
        }

        GatewayAopDynamicInstaller.install(filters.stream().map(GatewayFilter::getClass));
    }
}
