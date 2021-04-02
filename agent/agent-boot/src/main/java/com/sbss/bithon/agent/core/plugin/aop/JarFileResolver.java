package com.sbss.bithon.agent.core.plugin.aop;

import com.sbss.bithon.agent.core.expt.AgentException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 3:43 下午
 */
public class JarFileResolver {
    /**
     * resolve all jars under searchLocations
     */
    public static List<JarFileItem> resolve(File... directories) {
        List<JarFileItem> jarFiles = new LinkedList<>();
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
                    jarFiles.add(new JarFileItem(new JarFile(jar), jar));
                } catch (IOException e) {
                    throw new AgentException(e, "Exception when processing jar [%s]", fileName);
                }
            }
        }
        return jarFiles;
    }
}
