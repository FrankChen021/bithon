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

package org.bithon.agent.core.tracing;

import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.AppInstance;
import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.id.ISpanIdGenerator;
import org.bithon.agent.core.tracing.id.ITraceIdGenerator;
import org.bithon.agent.core.tracing.id.impl.DefaultSpanIdGenerator;
import org.bithon.agent.core.tracing.id.impl.UUIDGenerator;
import org.bithon.agent.core.tracing.propagation.DefaultPropagator;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;
import org.bithon.agent.core.tracing.reporter.ITraceReporter;
import org.bithon.agent.core.tracing.sampler.ISampler;
import org.bithon.agent.core.tracing.sampler.SamplerFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:42 下午
 */
public class Tracer {
    private static final ILogAdaptor log = LoggerFactory.getLogger(Tracer.class);

    private static volatile Tracer INSTANCE;

    private final String appName;
    private final String instanceName;
    private ITraceIdGenerator traceIdGenerator;
    private ITraceReporter reporter;
    private ITracePropagator propagator;
    private ISpanIdGenerator spanIdGenerator;

    public Tracer(String appName, String instanceName) {
        this.appName = appName;
        this.instanceName = instanceName;
    }

    public static Tracer get() {
        if (INSTANCE == null) {
            synchronized (Tracer.class) {
                if (INSTANCE == null) {

                    TraceConfig traceConfig = AgentContext.getInstance()
                                                          .getAgentConfiguration()
                                                          .getConfig(TraceConfig.class);

                    TraceConfig.SamplingConfig samplingConfig = traceConfig.getSamplingConfigs().get("default");
                    if (samplingConfig == null) {
                        samplingConfig = new TraceConfig.SamplingConfig();
                        samplingConfig.setSamplingRate(0);
                    }
                    AppInstance appInstance = AgentContext.getInstance().getAppInstance();
                    try {
                        ISampler sampler = SamplerFactory.createSampler(samplingConfig);
                        INSTANCE = new Tracer(appInstance.getQualifiedAppName(), appInstance.getHostAndPort())
                            .propagator(new DefaultPropagator(sampler))
                            .traceIdGenerator(new UUIDGenerator())
                            .spanIdGenerator(new DefaultSpanIdGenerator())
                            .reporter(samplingConfig.isDisabled() ? new NoopReporter() : new DefaultReporter());
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

    public Tracer spanIdGenerator(ISpanIdGenerator spanIdGenerator) {
        this.spanIdGenerator = spanIdGenerator;
        return this;
    }

    public ISpanIdGenerator spanIdGenerator() {
        return spanIdGenerator;
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
            try {
                traceDispatcher.send(traceMessages);
            } catch (Exception e) {
                log.error("exception when sending trace messages.", e);
            }
        }
    }
}
