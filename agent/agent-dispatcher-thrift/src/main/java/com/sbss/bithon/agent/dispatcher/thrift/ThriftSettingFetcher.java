package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.config.FetcherConfig;
import com.sbss.bithon.agent.core.setting.IAgentSettingFetcher;
import com.sbss.bithon.agent.rpc.thrift.service.setting.FetchRequest;
import com.sbss.bithon.agent.rpc.thrift.service.setting.FetchResponse;
import com.sbss.bithon.agent.rpc.thrift.service.setting.SettingService;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 4:01 下午
 */
public class ThriftSettingFetcher implements IAgentSettingFetcher {
    static Logger log = LoggerFactory.getLogger(ThriftSettingFetcher.class);

    private final AbstractThriftClient<SettingService.Client> client;

    public ThriftSettingFetcher(FetcherConfig config) {
        client = new AbstractThriftClient<SettingService.Client>("setting", config.getServers(), 3000) {
            @Override
            protected SettingService.Client createClient(TProtocol protocol) {
                return new SettingService.Client(protocol);
            }
        };
    }

    @Override
    public Map<String, String> fetch(String appName, String env, long lastModifiedSince) {

        client.ensureClient((client) -> {
            try {
                FetchResponse response = client.fetch(new FetchRequest(appName, env, lastModifiedSince));
                if (response.getStatusCode() != 200) {
                    throw new RuntimeException(new TApplicationException("Server returns code=" + response.getStatusCode() + ", Message=" + response.getMessage()));
                }

                return response.getSettings();
            } catch (TApplicationException e) {
                throw new RuntimeException(e.getMessage());
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }, 3);
        return null;
    }
}
