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

package org.bithon.agent.observability.event;

import org.bithon.agent.observability.exporter.Exporter;
import org.bithon.agent.observability.exporter.Exporters;

import java.util.Collections;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 27/12/22 2:29 pm
 */
public class ExceptionCollector {

    public static void collect(Throwable throwable) {
        collect(throwable, Collections.emptyMap());
    }

    public static void collect(Throwable throwable, Map<String, String> extraArgs) {
        collect(ExceptionBuilder.builder(extraArgs)
                                .exceptionClass(throwable.getClass())
                                .message(throwable.getMessage() == null ? "" : throwable.getMessage())
                                .stack(throwable));
    }

    public static void collect(ExceptionBuilder builder) {
        EventMessage exceptionEvent = new EventMessage("exception", builder.build());
        Exporter exporter = Exporters.getOrCreate(Exporters.DISPATCHER_NAME_EVENT);
        exporter.send(exporter.getMessageConverter().from(exceptionEvent));
    }

}
