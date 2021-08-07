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

package com.sbss.bithon.agent.core.tracing.context;

import com.sbss.bithon.agent.core.tracing.id.ISpanIdGenerator;
import com.sbss.bithon.agent.core.tracing.propagation.TraceMode;
import com.sbss.bithon.agent.core.tracing.propagation.injector.PropagationSetter;
import com.sbss.bithon.agent.core.tracing.reporter.ITraceReporter;
import com.sbss.bithon.agent.core.tracing.sampler.SamplingMode;
import com.sbss.bithon.agent.core.utils.time.Clock;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/17 3:01 下午
 */
public interface ITraceContext {

    TraceMode traceMode();

    String traceId();

    ITraceSpan currentSpan();

    Clock clock();

    ITraceReporter reporter();

    ITraceContext reporter(ITraceReporter reporter);

    ISpanIdGenerator spanIdGenerator();

    default ITraceSpan newSpan() {
        return newSpan(null, spanIdGenerator().newSpanId());
    }

    ITraceSpan newSpan(String parentSpanId, String spanId);

    void finish();

    ITraceContext samplingMode(SamplingMode mode);

    <T> void propagate(T injectedTo, PropagationSetter<T> setter);
}
