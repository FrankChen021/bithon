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

package org.bithon.agent.sentinel.servlet.filter;

import org.bithon.agent.observability.metric.domain.web.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.sentinel.ISentinelListener;
import org.bithon.agent.sentinel.degrade.DegradingRuleDto;
import org.bithon.agent.sentinel.flow.FlowRuleDto;

import java.util.Collection;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/5 23:50
 */
public class SentinelListener implements ISentinelListener {

    final HttpIncomingMetricsRegistry registry = HttpIncomingMetricsRegistry.get();

    @Override
    public void onDegraded(String uri, String method, Function<String, String> headerSupplier) {
        registry.getOrCreateMetrics(headerSupplier.apply(ITracePropagator.TRACE_HEADER_SRC_APPLICATION),
                                    method,
                                    uri,
                                    429)
                .getDegradedCount()
                .incr();
    }

    @Override
    public void onFlowControlled(String uri, String method, Function<String, String> headerSupplier) {
        registry.getOrCreateMetrics(headerSupplier.apply(ITracePropagator.TRACE_HEADER_SRC_APPLICATION),
                                    method,
                                    uri,
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
