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

package org.bithon.agent.java.adaptor;

import org.bithon.agent.instrumentation.expt.AgentException;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * JDK 8 specific adaptor implementation.
 * Since JDK 8 doesn't have the module system, this is mostly a no-op implementation.
 */
public class Java8Adaptor implements IJavaAdaptor {

    public Java8Adaptor(Instrumentation inst) {
    }

    @Override
    public void openPackages(Instrumentation inst,
                             Class<?> classFromSourceModule,
                             Map<String, Class<?>> openPackageTo) {
        // Do nothing for JDK 8
    }

    @Override
    public long getMaxDirectMemory() {
        try {
            Class<?> vmClass = Class.forName("sun.misc.VM");
            Method maxDirectMemoryMethod = vmClass.getDeclaredMethod("maxDirectMemory");
            maxDirectMemoryMethod.setAccessible(true);
            return (long) maxDirectMemoryMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new AgentException("sun.misc.VM.maxDirectMemory() not available", e);
        }
    }
}
