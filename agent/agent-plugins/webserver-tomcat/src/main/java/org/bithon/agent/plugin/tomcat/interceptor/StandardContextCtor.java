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

package org.bithon.agent.plugin.tomcat.interceptor;

import org.apache.catalina.core.StandardContext;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetricsCollector;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;
import org.bithon.agent.sentinel.ISentinelListener;
import org.bithon.agent.sentinel.degrade.DegradingRuleDto;
import org.bithon.agent.sentinel.flow.FlowRuleDto;
import org.bithon.agent.sentinel.servlet.SentinelFilter;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

/**
 * @author frankchen
 */
public class StandardContextCtor extends AbstractInterceptor {
    static ILogAdaptor log = LoggerFactory.getLogger(StandardContextCtor.class);

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

        HttpIncomingMetricsCollector collector;

        SentinelListener() {
            collector = MetricCollectorManager.getInstance()
                                              .getOrRegister("tomcat-web-request-metrics",
                                                             HttpIncomingMetricsCollector.class);

        }

        @Override
        public void onDegraded(HttpServletRequest request) {
            collector.getOrCreateMetrics(request.getHeader(ITracePropagator.TRACE_HEADER_SRC_APPLICATION),
                                         request.getRequestURI(),
                                         429)
                     .getDegradedCount()
                     .incr();
        }

        @Override
        public void onFlowControlled(HttpServletRequest request) {
            collector.getOrCreateMetrics(request.getHeader(ITracePropagator.TRACE_HEADER_SRC_APPLICATION),
                                         request.getRequestURI(),
                                         429)
                     .getFlowedCount()
                     .incr();
        }

        @Override
        public void onFlowRuleLoaded(String source, Collection<FlowRuleDto> rules) {
        }

        @Override
        public void onDegradeRuleLoaded(String source, Collection<DegradingRuleDto> rules) {
        }

        @Override
        public void onFlowRuleUnloaded(String source, Collection<FlowRuleDto> rules) {
        }

        @Override
        public void onDegradeRuleUnloaded(String source, Collection<DegradingRuleDto> rules) {
        }
    }
}
