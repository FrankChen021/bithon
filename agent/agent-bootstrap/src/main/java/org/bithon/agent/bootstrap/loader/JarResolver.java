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

package org.bithon.agent.bootstrap.loader;

import org.bithon.agent.bootstrap.expt.AgentException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 3:43 下午
 */
public class JarResolver {
    /**
     * resolve all jars under searchLocations
     */
    public static List<JarFile> resolve(File... directories) {
        List<JarFile> jarFiles = new ArrayList<>();
        for (File dir : directories) {
            if (!dir.exists() || !dir.isDirectory()) {
                continue;
            }

            String[] jarFileNames = dir.list((directory, name) -> name.endsWith(".jar"));
            if (jarFileNames == null) {
                continue;
            }

            for (String fileName : jarFileNames) {
                try {
                    File jar = new File(dir, fileName);
                    jarFiles.add(new JarFile(jar));
                } catch (IOException e) {
                    throw new AgentException(e, "Exception when processing jar [%s]", fileName);
                }
            }
        }
        return jarFiles;
    }
}
