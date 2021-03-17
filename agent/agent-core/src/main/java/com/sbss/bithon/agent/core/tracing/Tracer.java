package com.sbss.bithon.agent.core.tracing;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
import com.sbss.bithon.agent.core.tracing.context.ITraceIdGenerator;
import com.sbss.bithon.agent.core.tracing.context.impl.UUIDGenerator;
import com.sbss.bithon.agent.core.tracing.propagation.DefaultPropagator;
import com.sbss.bithon.agent.core.tracing.propagation.ITracePropagator;
import com.sbss.bithon.agent.core.tracing.report.ITraceReporter;
import com.sbss.bithon.agent.core.tracing.sampling.ISamplingDecisionMaker;
import com.sbss.bithon.agent.core.tracing.sampling.RatioSamplingDecisionMaker;
import shaded.org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:42 下午
 */
public class Tracer {
    private static Tracer INSTANCE;

    private final String appName;
    private final String instanceName;
    private ITraceIdGenerator traceIdGenerator;
    private ITraceReporter reporter;
    private ITracePropagator propagator;
    private ISamplingDecisionMaker samplingDecisionMaker;

    public Tracer(String appName, String instanceName) {
        this.appName = appName;
        this.instanceName = instanceName;
    }

    public static Tracer get() {
        if (INSTANCE == null) {
            synchronized (Tracer.class) {
                if (INSTANCE == null) {
                    AppInstance appInstance = AgentContext.getInstance().getAppInstance();
                    try {
                        final Dispatcher traceDispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_TRACING);

                        INSTANCE = new Tracer(appInstance.getAppName(),
                                              appInstance.getHostIp() + ":" + appInstance.getPort())
                            .propagator(new DefaultPropagator())
                            .traceIdGenerator(new UUIDGenerator())
                            .reporter((spans) -> {
                                List<Object> traceMessages = spans.stream()
                                                                  .map(span -> traceDispatcher.getMessageConverter()
                                                                                              .from(span))
                                                                  .collect(Collectors.toList());
                                traceDispatcher.sendMessage(traceMessages);
                            })
                            .samplingDecisionMaker(new RatioSamplingDecisionMaker(100));
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

    public ISamplingDecisionMaker samplingDecisionMaker() {
        return this.samplingDecisionMaker;
    }

    public Tracer samplingDecisionMaker(ISamplingDecisionMaker decisionMaker) {
        this.samplingDecisionMaker = decisionMaker;
        return this;
    }
}
