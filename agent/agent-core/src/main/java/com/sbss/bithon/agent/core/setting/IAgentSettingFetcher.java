package com.sbss.bithon.agent.core.setting;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 2:45 下午
 */
public interface IAgentSettingFetcher {
    Map<String, String> fetch(String appName,
                              String env,
                              long lastModifiedSince);
}
