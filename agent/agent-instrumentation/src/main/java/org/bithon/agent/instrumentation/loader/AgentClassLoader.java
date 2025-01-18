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

package org.bithon.agent.instrumentation.loader;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.utils.AgentDirectory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/31 21:44
 */
public class AgentClassLoader {

    private static volatile JarClassLoader instance;

    public static JarClassLoader getClassLoader() {
        if (instance == null) {
            synchronized (AgentClassLoader.class) {
                if (instance == null) {
                    final Thread mainThread = Thread.currentThread();
                    instance = new JarClassLoader("agent-library",
                                                  JarResolver.resolve(new LibraryJarFilter(), AgentDirectory.getSubDirectory("lib")),
                                                  mainThread::getContextClassLoader);
                }
            }
        }
        return instance;
    }

    /**
     * For jars located under the 'lib' directory, it MUST be either:
     * an agent jar which starts with 'agent-'
     * a component jar which starts with 'component-'
     * a shaded jar which starts with 'shaded-'
     * <p>
     * If a jar is not one of the above, usually it means an agent module introduces extra dependencies.
     * For an agent module, especially a plugin module,
     * external dependencies should be introduced by the scope defined as 'provided'
     */
    private static class LibraryJarFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            if (name.startsWith("agent-")
                || name.startsWith("component-")
                || name.startsWith("shaded-")) {
                {
                    return true;
                }
            }

            throw new AgentException(String.format(Locale.ENGLISH,
                                                   "Unexpected jar [%s] under the agent library found. Contact developers to check the dependencies of the agent and make a fix.", name));
        }
    }
}
