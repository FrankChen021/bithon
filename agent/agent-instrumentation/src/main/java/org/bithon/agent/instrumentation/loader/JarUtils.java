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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 3:44 下午
 */
public final class JarUtils {

    public static URL getClassURL(JarFile jar, String className) throws IOException {
        return new URL("jar:file:" + jar.getName() + "!/" + className);
    }

    public static byte[] openClassFile(JarFile jar, String className) throws IOException {
        URL classURL = getClassURL(jar, className);
        try (InputStream inputStream = classURL.openStream()) {
            return toByte(inputStream);
        }
    }

    public static byte[] toByte(InputStream input) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(input);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            int len;
            byte[] buffer = new byte[2048];
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }
}
