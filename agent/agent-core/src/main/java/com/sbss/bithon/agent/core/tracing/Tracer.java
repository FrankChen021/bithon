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

package com.sbss.bithon.agent.core.tracing;

import com.sbss.bithon.agent.core.config.AgentConfigManager;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
import com.sbss.bithon.agent.core.tracing.config.TraceConfig;
import com.sbss.bithon.agent.core.tracing.context.ITraceIdGenerator;
import com.sbss.bithon.agent.core.tracing.context.ITraceSpan;
import com.sbss.bithon.agent.core.tracing.context.impl.UUIDGenerator;
import com.sbss.bithon.agent.core.tracing.propagation.DefaultPropagator;
import com.sbss.bithon.agent.core.tracing.propagation.ITracePropagator;
import com.sbss.bithon.agent.core.tracing.reporter.ITraceReporter;
import com.sbss.bithon.agent.core.tracing.sampler.ISampler;
import com.sbss.bithon.agent.core.tracing.sampler.SamplerFactory;
import shaded.org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:42 下午
 */
public class Tracer {
    private static volatile Tracer INSTANCE;

    private final String appName;
    private final String instanceName;
    private ITraceIdGenerator traceIdGenerator;
    private ITraceReporter reporter;
    private ITracePropagator propagator;
    private ISampler sampler;

    public Tracer(String appName, String instanceName) {
        this.appName = appName;
        this.instanceName = instanceName;
    }

    static class NoopReporter implements ITraceReporter {
        @Override
        public void report(List<ITraceSpan> spans) {
        }
    }

    static class DefaultReporter implements ITraceReporter {
        private final Dispatcher traceDispatcher;

        public DefaultReporter() {
            traceDispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_TRACING);
        }

        @Override
        public void report(List<ITraceSpan> spans) {
            List<Object> traceMessages = spans.stream()
                                              .map(span -> traceDispatcher.getMessageConverter().from(span))
                                              .filter(Objects::nonNull)
                                              .collect(Collectors.toList());
            traceDispatcher.sendMessage(traceMessages);
        }
    }

    public static Tracer get() {
        if (INSTANCE == null) {
            synchronized (Tracer.class) {
                if (INSTANCE == null) {

                    TraceConfig traceConfig = AgentConfigManager.getInstance().getConfig(TraceConfig.class);

                    AppInstance appInstance = AgentContext.getInstance().getAppInstance();
                    try {
                        INSTANCE = new Tracer(appInstance.getAppName(),
                                              appInstance.getHostIp() + ":" + appInstance.getPort())
                            .propagator(new DefaultPropagator())
                            .traceIdGenerator(new UUIDGenerator())
                            .reporter(traceConfig.isDisabled() ? new NoopReporter() : new DefaultReporter())
                            .sampler(SamplerFactory.createSampler(traceConfig));
                    } catch (Exception e) {
                        LoggerFactory.getLogger(Tracer.class).info("Failed to create Tracer", e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    public Tracer traceIdGenerator(ITraceIdGenerator idGenerator) {
        this.traceIdGenerator = idGenerator;
        return this;
    }

    public ITraceIdGenerator traceIdGenerator() {
        return this.traceIdGenerator;
    }

    public Tracer reporter(ITraceReporter reporter) {
        this.reporter = reporter;
        return this;
    }

    public ITraceReporter reporter() {
        return reporter;
    }

    public ITracePropagator propagator() {
        return propagator;
    }

    public Tracer propagator(ITracePropagator propagator) {
        this.propagator = propagator;
        return this;
    }

    public String appName() {
        return appName;
    }

    public String instanceName() {
        return instanceName;
    }

    public ISampler sampler() {
        return this.sampler;
    }

    public Tracer sampler(ISampler sampler) {
        this.sampler = sampler;
        return this;
    }
}
