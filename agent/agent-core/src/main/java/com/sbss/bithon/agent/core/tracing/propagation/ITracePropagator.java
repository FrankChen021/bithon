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

package com.sbss.bithon.agent.core.tracing.propagation;

import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.propagation.extractor.PropagationGetter;
import com.sbss.bithon.agent.core.tracing.propagation.injector.PropagationSetter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:36 下午
 */
public interface ITracePropagator {

    String BITHON_SOURCE_APPLICATION = "X-Bithon-Application";
    String BITHON_TRACE_ID = "BITHON-TRACE-ID";
    String BITHON_SPAN_IDS = "BITHON-SPAN-IDS";
    String BITHON_ID_SEPARATOR = ";";

    <R> void inject(TraceContext context, R request, PropagationSetter<R> setter);

    <R> TraceContext extract(R request, PropagationGetter<R> getter);
}
