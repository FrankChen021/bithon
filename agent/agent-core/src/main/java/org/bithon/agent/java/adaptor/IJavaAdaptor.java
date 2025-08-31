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

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for JDK-specific adaptations in the agent.
 * Implementations handle differences between various JDK versions.
 */
public interface IJavaAdaptor {

    default void openPackages(Instrumentation inst,
                              Class<?> classFromSourceModule,
                              String packageToOpen,
                              Class<?> targetClass) {
        Map<String, Class<?>> openPackageTo = new HashMap<>();
        openPackageTo.put(packageToOpen, targetClass);

        openPackages(inst, classFromSourceModule, openPackageTo);
    }

    /**
     * Open necessary modules and packages for agent operation.
     * This handles JDK-specific module system requirements.
     *
     * @param inst the instrumentation instance
     */
    void openPackages(Instrumentation inst,
                      Class<?> classFromSourceModule,
                      Map<String, Class<?>> openPackageTo);

    long getMaxDirectMemory();

    String getModuleName(Class<?> clazz);
}
