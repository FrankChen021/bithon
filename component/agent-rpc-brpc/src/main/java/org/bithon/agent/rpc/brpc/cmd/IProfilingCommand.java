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

package org.bithon.agent.rpc.brpc.cmd;


import org.bithon.component.brpc.BrpcService;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.message.serializer.Serializer;

/**
 * @author frank.chen021@outlook.com
 * @date 26/5/25 3:37 pm
 */
@BrpcService(name = "agent.profiling", serializer = Serializer.JSON_SMILE)
public interface IProfilingCommand {

    class ProfilingRequest {
        public int durationInSeconds = 60; // default to 60 seconds
    }

    class ProfilingFrame {

    }

    /**
     * Starts the profiling session and streams profiling frames to the provided response.
     *
     * @param response The response object to which profiling frames will be streamed.
     */
    void start(ProfilingRequest request, StreamResponse<ProfilingFrame> response);
}
