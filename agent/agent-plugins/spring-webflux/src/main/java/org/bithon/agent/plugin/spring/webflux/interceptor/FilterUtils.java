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

import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.plugin.spring.webflux.config.GatewayFilterConfigs;
import org.bithon.component.commons.utils.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 13/2/22 3:28 PM
 */
public class FilterUtils {

    public static void extractAttributesAsTraceTags(ServerWebExchange exchange, GatewayFilterConfigs configs, Class<?> filterClass, ITraceSpan span) {
        // get configuration of this filter
        final GatewayFilterConfigs.Filter filterConfig = configs.get(filterClass.getName());
        if (CollectionUtils.isEmpty(filterConfig.getAttributes())) {
            return;
        }

        for (Map.Entry<String, String> entry : filterConfig.getAttributes().entrySet()) {
            Object attribValue = exchange.getAttribute(entry.getKey());
            if (attribValue != null) {
                span.tag(entry.getValue(), attribValue.toString());
            }
        }
    }
}
