package com.sbss.bithon.agent.core.setting;

import java.util.Map;

/**
 * @author frankchen
 * @date 2020-05-27 14:41:22
 */
public interface IAgentSettingRefreshListener {
    void onRefresh(Map<String, Object> pluginSetting);
}
