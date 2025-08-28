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


import org.bithon.agent.controller.cmd.profiling.asyncprofiler.AsyncProfilerProvider;
import org.bithon.agent.instrumentation.loader.AgentClassLoader;

import java.lang.reflect.InvocationTargetException;

/**
 * @author frank.chen021@outlook.com
 * @date 16/7/25 8:11 pm
 */
public class ProfilerFactory {

    public static IProfilerProvider create() {
        if (!AgentClassLoader.class.getName().equals(ProfilerFactory.class.getClassLoader().getClass().getName())) {
            try {
                Class<?> impl = AgentClassLoader.getClassLoader().loadClass("org.bithon.agent.controller.cmd.profiling.asyncprofiler.AsyncProfilerProvider");
                return (IProfilerProvider) impl.getConstructor().newInstance();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getTargetException() == null ? e : e.getTargetException());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return new AsyncProfilerProvider();
    }
}
