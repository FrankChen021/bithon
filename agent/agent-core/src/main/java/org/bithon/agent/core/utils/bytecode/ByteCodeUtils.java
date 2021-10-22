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

package org.bithon.agent.core.utils.bytecode;

import org.bithon.agent.bootstrap.expt.AgentException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/11 11:35
 */
public class ByteCodeUtils {

    public static byte[] getClassByteCode(String className, ClassLoader classLoader) {
        String classResourceName = className.replaceAll("\\.", "/") + ".class";
        try {
            try (InputStream resourceAsStream = classLoader.getResourceAsStream(classResourceName)) {
                if (resourceAsStream == null) {
                    throw new AgentException("Class [%s] for bootstrap injection not found", className);
                }

                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[2048];
                    int len;

                    while ((len = resourceAsStream.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }

                    return os.toByteArray();
                }
            }
        } catch (IOException e) {
            throw new AgentException("Failed to load class [%s]: %s", className, e.getMessage());
        }
    }
}
