package com.sbss.bithon.agent.core.util;

/**
 * @author frankchen
 * @Date 2020-03-17 15:50:34
 */
public class UserAgentFilter {

    private static final String IGNORE_USER_AGENT = "Mozilla/5.0 KERUYUN Cloud Security Scanner tp_security@keruyun.com";

    /**
     * 根据UserAgent过滤请求
     *
     * @param userAgent
     * @return
     */
    public static boolean isFiltered(String userAgent) {
        if (userAgent != null && userAgent.equals(IGNORE_USER_AGENT))
            return true;
        else
            return false;
    }
}
