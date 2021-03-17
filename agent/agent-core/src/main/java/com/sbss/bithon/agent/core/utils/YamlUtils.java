package com.sbss.bithon.agent.core.utils;

import shaded.org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;

/**
 * @author frankchen
 */
public class YamlUtils {

    public static <T> T load(String yml,
                             Class<T> clazz) throws Exception {
        try (InputStream is = URLClassLoader.getSystemResourceAsStream(yml)) {
            if (is == null) {
                throw new FileNotFoundException(yml);
            }
            return new Yaml().loadAs(is, clazz);
        }
    }

    public static <T> T load(File yml,
                             Class<T> clazz) throws IOException {
        try (InputStream is = new FileInputStream(yml)) {
            return new Yaml().loadAs(is, clazz);
        }
    }

    public static <T> T load(String yml,
                             Class<T> clazz,
                             ClassLoader classLoader) throws Exception {
        try (InputStream is = classLoader.getResourceAsStream(yml)) {
            if (is == null) {
                throw new FileNotFoundException(yml);
            }
            return new Yaml().loadAs(is, clazz);
        }
    }
}
