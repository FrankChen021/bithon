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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 3:43 下午
 */
public class JarResolver {
    /**
     * resolve all jars under searchLocations
     */
    public static List<Jar> resolve(File... directories) {
        return resolve(null, directories);
    }

    public static List<Jar> resolve(FilenameFilter predicate, File... directories) {
        List<Jar> jars = new ArrayList<>();
        for (File dir : directories) {
            if (!dir.exists() || !dir.isDirectory()) {
                continue;
            }

            String[] jarFileNames = dir.list((directory, name) -> {
                if (!name.endsWith(".jar")) {
                    return false;
                }
                return predicate == null || predicate.accept(directory, name);
            });
            if (jarFileNames == null) {
                continue;
            }

            for (String fileName : jarFileNames) {
                try {
                    jars.add(new Jar(new File(dir, fileName)));
                } catch (IOException e) {
                    throw new AgentException(e, "Exception when processing jar [%s]", fileName);
                }
            }
        }
        return jars;
    }
}
