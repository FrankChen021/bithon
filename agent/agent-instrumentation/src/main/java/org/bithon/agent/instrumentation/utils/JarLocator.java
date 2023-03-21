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

package org.bithon.agent.instrumentation.utils;

import org.bithon.agent.instrumentation.expt.AgentException;

import java.io.File;
import java.net.URL;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/3 00:22
 */
public class JarLocator {
    /**
     * @return The jar file location that contains the specified class
     */
    public static File locate(String className) {
        final String internalClassName = className.replace('.', '/') + ".class";
        final URL classURL = ClassLoader.getSystemResource(internalClassName);
        if (classURL == null) {
            throw new AgentException("Unable to locate class [%s]", className);
        }

        if (!"jar".equals(classURL.getProtocol())) {
            throw new AgentException("Unknown agent-instrumentation.jar location: %s", classURL.toString());
        }

        /**
         * classURL.getPath returns a path as below
         * file:/directory/dest/agent-instrumentation.jar!/com/sbss/bithon/agent/bootstrap/AgentApp.class
         *
         * and we extract agent jar file path from it
         */
        String path = classURL.getPath();
        int jarIndex = path.indexOf("!/");
        if (jarIndex == -1) {
            throw new AgentException("Invalid path [%s]" + path);
        }
        return new File(path.substring("file:".length(), jarIndex));
    }
}
