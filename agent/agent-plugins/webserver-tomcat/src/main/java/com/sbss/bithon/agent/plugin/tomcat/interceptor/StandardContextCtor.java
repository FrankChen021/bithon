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

package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.plugin.tomcat.metric.WebRequestMetricCollector;
import com.sbss.bithon.agent.sentinel.ISentinelListener;
import com.sbss.bithon.agent.sentinel.degrade.DegradeRuleDto;
import com.sbss.bithon.agent.sentinel.flow.FlowRuleDto;
import com.sbss.bithon.agent.sentinel.servlet.SentinelFilter;
import org.apache.catalina.core.StandardContext;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

/**
 * @author frankchen
 */
public class StandardContextCtor extends AbstractInterceptor {
    static Logger log = LoggerFactory.getLogger(StandardContextCtor.class);

    /**
     * {@link StandardContext#StandardContext()}
     */
    @Override
    public void onConstruct(AopContext aopContext) {
        StandardContext servletContext = aopContext.castTargetAs();

        //
        // register sentinel filter
        //
        servletContext.addServletContainerInitializer((c, ctx) -> {
            try {
                SentinelListener listener = new SentinelListener();
                ctx.addFilter("com.sbss.bithon.agent.sentinel", new SentinelFilter(listener))
                   .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
                log.info("Sentinel for tomcat installed");
            } catch (Exception e) {
                log.error("Exception occurred when initialize servlet context. sentinel may not be installed", e);
            }
        }, Collections.emptySet());
    }

    static class SentinelListener implements ISentinelListener {

        WebRequestMetricCollector counter;

        SentinelListener() {
            counter = MetricCollectorManager.getInstance()
                                            .getOrRegister("tomcat-web-request-metrics",
                                                           WebRequestMetricCollector.class);

        }

        @Override
        public void onDegraded(HttpServletRequest request) {
            counter.getOrCreate(request).getDegradedCount().incr();
        }

        @Override
        public void onFlowControlled(HttpServletRequest request) {
            counter.getOrCreate(request).getFlowedCount().incr();
        }

        @Override
        public void onFlowRuleLoaded(String source, Collection<FlowRuleDto> rules) {
        }

        @Override
        public void onDegradeRuleLoaded(String source, Collection<DegradeRuleDto> rules) {
        }

        @Override
        public void onFlowRuleUnloaded(String source, Collection<FlowRuleDto> rules) {
        }

        @Override
        public void onDegradeRuleUnloaded(String source, Collection<DegradeRuleDto> rules) {
        }
    }
}
