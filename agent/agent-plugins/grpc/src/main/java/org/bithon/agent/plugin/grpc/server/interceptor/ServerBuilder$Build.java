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

package org.bithon.agent.plugin.grpc.server.interceptor;

import io.grpc.ServerBuilder;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.propagation.extractor.ChainedTraceContextExtractor;
import org.bithon.agent.core.tracing.sampler.SamplerFactory;

/**
 * @author Frank Chen
 * @date 22/12/22 3:54 pm
 */
public class ServerBuilder$Build extends AbstractInterceptor {

    private final ChainedTraceContextExtractor contextExtractor;

    public ServerBuilder$Build() {
        TraceConfig traceConfig = AgentContext.getInstance()
                                              .getAgentConfiguration()
                                              .getConfig(TraceConfig.class);

        contextExtractor = new ChainedTraceContextExtractor(SamplerFactory.createSampler(traceConfig.getSamplingConfigs().get("grpc")));
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {

        ServerBuilder<?> builder = aopContext.castTargetAs();
        builder.intercept(new ServerCallInterceptor(contextExtractor));

        return InterceptionDecision.SKIP_LEAVE;
    }
}
