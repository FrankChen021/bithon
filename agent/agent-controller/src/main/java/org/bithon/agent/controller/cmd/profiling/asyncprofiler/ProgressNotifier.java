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

package org.bithon.agent.controller.cmd.profiling.asyncprofiler;


import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.Progress;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 22/8/25 5:06 pm
 */
public class ProgressNotifier {
    private final StreamResponse<ProfilingEvent> streamResponse;

    public ProgressNotifier(StreamResponse<ProfilingEvent> streamResponse) {
        this.streamResponse = streamResponse;
    }

    public void sendProgress(String message) {
        ProfilingEvent event = ProfilingEvent.newBuilder()
                                             .setProgress(Progress.newBuilder()
                                                                  .setTimestamp(System.currentTimeMillis())
                                                                  .setMessage(message)
                                                                  .build())
                                             .build();
        streamResponse.onNext(event);
    }

    public void sendProgress(String messageFormat, Object... args) {
        ProfilingEvent event = ProfilingEvent.newBuilder()
                                             .setProgress(Progress.newBuilder()
                                                                  .setTimestamp(System.currentTimeMillis())
                                                                  .setMessage(StringUtils.format(messageFormat, args))
                                                                  .build())
                                             .build();
        streamResponse.onNext(event);
    }
}
