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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Represents a JAR file with its location URL.
 * This is used to maintain both the JarFile instance and its location
 * for proper CodeSource information when loading classes.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/10/15
 */
public class Jar {
    private final JarFile jarFile;
    private final URL url;

    public Jar(File jarFile) throws IOException {
        this.jarFile = new JarFile(jarFile);
        this.url = jarFile.toURI().toURL();
    }

    public JarFile getJarFile() {
        return jarFile;
    }

    public URL getURL() {
        return url;
    }

    /**
     *
     * @param classPath e.g. com/bithon/agent/SomeClass.class
     */
    public byte[] getClassByteCode(String classPath) throws IOException {
        JarEntry entry = jarFile.getJarEntry(classPath);
        if (entry == null) {
            return null;
        }

        URL classURL = JarUtils.getClassURL(jarFile, classPath);
        try (InputStream inputStream = classURL.openStream()) {
            return JarUtils.toByte(inputStream);
        }
    }

    @Override
    public String toString() {
        return "Jar{" +
               "jarFile=" + jarFile.getName() +
               ", url=" + url +
               '}';
    }
}

