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

package org.bithon.agent.controller.cmd;


import org.bithon.agent.controller.cmd.profiling.IProfilerProvider;
import org.bithon.agent.controller.cmd.profiling.ProfilerFactory;
import org.bithon.agent.rpc.brpc.cmd.IProfilingCommand;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.component.brpc.StreamResponse;

/**
 * @author frank.chen021@outlook.com
 * @date 16/7/25 8:51 pm
 */
public class ProfilingCommand implements IProfilingCommand, IAgentCommand {

    private volatile IProfilerProvider provider;

    @Override
    public void start(ProfilingRequest request, StreamResponse<ProfilingEvent> response) {
        if (provider == null) {
            synchronized (this) {
                if (provider == null) {
                    provider = ProfilerFactory.create();
                }
            }
        }
        try {
            provider.start(request, response);
        } catch (Throwable e) {
            response.onException(e);
        }
    }

    @Override
    public void stop() {
        if (provider != null) {
            provider.stop();
        }
    }

    @Override
    public String getStatus() {
        return provider != null && provider.isRunning() ? "RUNNING" : "IDLE";
    }
}
