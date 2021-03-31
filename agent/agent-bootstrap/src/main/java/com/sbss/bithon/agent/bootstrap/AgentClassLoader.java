package com.sbss.bithon.agent.bootstrap;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/31 21:44
 */
public class AgentClassLoader extends URLClassLoader {
    public AgentClassLoader(String dir) {
        super(Arrays.stream(new File(dir).list((directory, name) -> name.endsWith(".jar"))).map(jar -> {
            try {
                return new File(dir + '/' + jar).toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }).collect(Collectors.toList()).toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }
}
