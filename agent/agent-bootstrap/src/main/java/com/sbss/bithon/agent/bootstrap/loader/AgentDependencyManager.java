/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.bootstrap.loader;

import java.io.File;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/31 21:44
 */
public class AgentDependencyManager {

    private static JarClassLoader instance;

    public static ClassLoader getClassLoader() {
        return instance;
    }

    /**
     * initialize class loader as a cascaded class loader
     * it's parent is context class loader of thread so that any classes used by jars in libs could be found by application's class loader
     */
    public static ClassLoader initialize(File agentDirectory) {
        final Thread mainThread = Thread.currentThread();
        instance = new JarClassLoader("agent-bootstrap",
                                      JarResolver.resolve(new File(agentDirectory, "lib")),
                                      mainThread::getContextClassLoader);
        return instance;
    }
}
