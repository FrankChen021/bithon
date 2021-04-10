package com.sbss.bithon.agent.core.utils;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/21 22:28
 */
public class MiscUtils {

    /**
     * clean up parameters on connection string
     * <p>
     * We don't parse DB,HostAndPort from connection string at agent side
     * because the rules are a little bit complex which would cause more frequent upgrading of agent
     */
    public static String cleanupConnectionString(String connectionString) {
        return connectionString.split("\\?")[0].split(";")[0];
    }
}
