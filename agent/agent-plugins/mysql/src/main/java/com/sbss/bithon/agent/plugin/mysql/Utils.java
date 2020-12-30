package com.sbss.bithon.agent.plugin.mysql;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/27 10:35 下午
 */
public class Utils {
    public static String extractHostAndPort(String jdbcConnectionString) throws URISyntaxException {
        String originUrl = jdbcConnectionString.replaceFirst("jdbc:", "");
        URI uri = new URI(originUrl);
        return uri.getHost() + ":" + uri.getPort();
    }
}
