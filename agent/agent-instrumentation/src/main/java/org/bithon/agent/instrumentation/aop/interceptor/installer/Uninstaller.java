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

package org.bithon.agent.instrumentation.aop.interceptor.installer;

import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;
import org.bithon.shaded.net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.lang.instrument.Instrumentation;

/**
 * @author Frank Chen
 * @date 2/2/24 6:22 pm
 */
public class Uninstaller {
    private final ResettableClassFileTransformer transformer;
    private final Instrumentation instance;

    public Uninstaller(ResettableClassFileTransformer transformer, Instrumentation instance) {
        this.transformer = transformer;
        this.instance = instance;
    }

    public void uninstall() {
        transformer.reset(instance, AgentBuilder.RedefinitionStrategy.REDEFINITION);

        // TODO: remove records from InstallerRecorder
    }
}
