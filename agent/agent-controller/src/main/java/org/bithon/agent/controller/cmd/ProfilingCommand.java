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


import org.bithon.agent.instrumentation.loader.AgentClassLoader;
import org.bithon.agent.instrumentation.loader.JarClassLoader;
import org.bithon.agent.instrumentation.utils.AgentDirectory;
import org.bithon.agent.rpc.brpc.cmd.IProfilingCommand;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.agent.rpc.brpc.profiling.ProfilingResponse;
import org.bithon.component.brpc.StreamResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author frank.chen021@outlook.com
 * @date 16/7/25 8:51 pm
 */
public class ProfilingCommand implements IProfilingCommand, IAgentCommand {
    private final JarClassLoader classLoader = new JarClassLoader("async-profiler",
                                                                  AgentDirectory.getSubDirectory("tools/async-profiler"),
                                                                  AgentClassLoader.getClassLoader());

    @Override
    public void start(ProfilingRequest request, StreamResponse<ProfilingResponse> response) {
        try {
            Class<?> clazz = Class.forName("org.bithon.agent.controller.cmd.profiling.Profiler", true, classLoader);
            Method method = clazz.getDeclaredMethod("start", ProfilingRequest.class, StreamResponse.class);
            method.invoke(null, request, response);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            response.onException(e);
        } catch (InvocationTargetException e) {
            response.onException(e.getCause());
        }
    }
}
