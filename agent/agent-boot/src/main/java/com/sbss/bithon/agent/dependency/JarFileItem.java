package com.sbss.bithon.agent.dependency;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 3:44 下午
 */
public final class JarFileItem {
    final JarFile jarFile;
    final File sourceFile;

    JarFileItem(JarFile jarFile, File filePath) {
        this.jarFile = jarFile;
        this.sourceFile = filePath;
    }

    byte[] openClassFile(String classFile) throws IOException {
        URL classFileUrl = new URL("jar:file:" + sourceFile.getAbsolutePath() + "!/" + classFile);

        try (BufferedInputStream inputStream = new BufferedInputStream(classFileUrl.openStream());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[2048];
            int len;

            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }

    public JarFile getJarFile() {
        return jarFile;
    }

    public File getSourceFile() {
        return sourceFile;
    }
}
