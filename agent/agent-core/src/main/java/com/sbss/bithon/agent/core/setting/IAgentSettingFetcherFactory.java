package com.sbss.bithon.agent.core.setting;

import com.sbss.bithon.agent.core.config.FetcherConfig;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 4:40 下午
 */
public interface IAgentSettingFetcherFactory {
    IAgentSettingFetcher createFetcher(FetcherConfig config);
}
