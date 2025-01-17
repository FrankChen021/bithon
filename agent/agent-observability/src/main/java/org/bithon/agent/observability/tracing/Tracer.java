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

package org.bithon.agent.observability.tracing;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.exporter.Exporter;
import org.bithon.agent.observability.exporter.Exporters;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.config.TraceSamplingConfig;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.propagation.DefaultPropagator;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;
import org.bithon.agent.observability.tracing.id.ITraceIdGenerator;
import org.bithon.agent.observability.tracing.id.impl.DefaultSpanIdGenerator;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.agent.observability.tracing.sampler.ISampler;
import org.bithon.agent.observability.tracing.sampler.SamplerFactory;
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
    private ITraceReporter reporter;
    private ITracePropagator propagator;
    private ISpanIdGenerator spanIdGenerator;
    private TraceConfig traceConfig;

    public Tracer(String appName, String instanceName) {
        this.appName = appName;
        this.instanceName = instanceName;
    }

    public static Tracer get() {
        if (INSTANCE == null) {
            synchronized (Tracer.class) {
                if (INSTANCE == null) {
                    AppInstance appInstance = AppInstance.getInstance();
                    try {
                        TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

                        ISampler sampler = SamplerFactory.createSampler(ConfigurationManager.getInstance()
                                                                                            .getDynamicConfig("tracing.samplingConfigs.default",
                                                                                                              TraceSamplingConfig.class));
                        INSTANCE = new Tracer(appInstance.getQualifiedName(), appInstance.getInstanceName())
                            .traceConfig(traceConfig)
                            .propagator(new DefaultPropagator(sampler))
                            .spanIdGenerator(new DefaultSpanIdGenerator())
                            .reporter(new DefaultReporter());
                    } catch (Exception e) {
                        LoggerFactory.getLogger(Tracer.class).info("Failed to create Tracer", e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    public Tracer traceConfig(TraceConfig traceConfig) {
        this.traceConfig = traceConfig;
        return this;
    }

    public boolean disabled() {
        return this.traceConfig.isDisabled();
    }

    public ITraceIdGenerator traceIdGenerator() {
        // Always return the traceIdGenerator from traceConfig
        // so that dynamic change of traceConfig can take effect
        return this.traceConfig.getTraceIdGenerator();
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
        private final Exporter traceExporter;

        public DefaultReporter() {
            traceExporter = Exporters.getOrCreate(Exporters.EXPORTER_NAME_TRACING);
        }

        @Override
        public void report(List<ITraceSpan> spans) {
            List<Object> traceMessages = spans.stream()
                                              .map(span -> traceExporter.getMessageConverter().from(span))
                                              .filter(Objects::nonNull)
                                              .collect(Collectors.toList());
            try {
                traceExporter.send(traceMessages);
            } catch (Exception e) {
                log.error("exception when sending trace messages.", e);
            }
        }
    }
}
