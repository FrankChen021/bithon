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

package org.bithon.agent.controller.cmd.profiling;


import org.bithon.agent.instrumentation.loader.AgentClassLoader;
import org.bithon.agent.instrumentation.loader.JarClassLoader;
import org.bithon.agent.instrumentation.utils.AgentDirectory;

/**
 * @author frank.chen021@outlook.com
 * @date 16/7/25 8:11 pm
 */
public class ProfilerFactory {

    private static final JarClassLoader CLASS_LOADER = new JarClassLoader("async-profiler",
                                                                          AgentDirectory.getSubDirectory("tools/async-profiler"),
                                                                          AgentClassLoader.getClassLoader());

    public static IProfilerProvider create() throws Throwable {
        Class<?> clazz = Class.forName("org.bithon.agent.controller.cmd.profiling.asyncprofiler.AsyncProfilerProvider",
                                       true,
                                       CLASS_LOADER);
        return (IProfilerProvider) clazz.getConstructor().newInstance();
    }
}
