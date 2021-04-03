package com.sbss.bithon.agent.bootstrap;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        try (BufferedInputStream inputStream = new BufferedInputStream(classURL.openStream());
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
