package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.config.FetcherConfig;
import com.sbss.bithon.agent.core.setting.IAgentSettingFetcher;
import com.sbss.bithon.agent.core.setting.IAgentSettingFetcherFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 4:40 下午
 */
public class SettingFetcherFactory implements IAgentSettingFetcherFactory {
    @Override
    public IAgentSettingFetcher createFetcher(FetcherConfig config) {
        return new ThriftSettingFetcher(config);
    }
}
