package com.sbss.bithon.agent.core.util;

import shaded.org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URLClassLoader;

public class YamlUtil {

    public static <T> T load(String yml,
                             Class<T> clazz) throws Exception {
        T result;
        InputStream is = null;
        try {
            is = URLClassLoader.getSystemResourceAsStream(yml);
            if (is == null) {
                throw new FileNotFoundException(yml);
            }
            result = new Yaml().loadAs(is, clazz);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

    public static <T> T load(File yml,
                             Class<T> clazz) throws Exception {
        T result;
        InputStream is = null;
        try {
            is = new FileInputStream(yml);
            result = new Yaml().loadAs(is, clazz);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

    public static <T> T load(String yml,
                             Class<T> clazz,
                             ClassLoader classLoader) throws Exception {
        T result;
        InputStream is = null;
        try {
            is = classLoader.getResourceAsStream(yml);
            if (is == null) {
                throw new FileNotFoundException(yml);
            }
            result = new Yaml().loadAs(is, clazz);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

}
