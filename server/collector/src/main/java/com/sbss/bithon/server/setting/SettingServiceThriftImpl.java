package com.sbss.bithon.server.setting;

import com.sbss.bithon.agent.rpc.thrift.service.setting.FetchRequest;
import com.sbss.bithon.agent.rpc.thrift.service.setting.FetchResponse;
import com.sbss.bithon.agent.rpc.thrift.service.setting.SettingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 7:03 下午
 */
@Slf4j
@Service
public class SettingServiceThriftImpl implements SettingService.Iface {

    private final AgentSettingService service;

    public SettingServiceThriftImpl(AgentSettingService service) {
        this.service = service;
    }

    @Override
    public FetchResponse fetch(FetchRequest request) throws TException {
        log.info("Fetching {} {}", request.getAppName(), request.getEnvName());
        try {
            return new FetchResponse(200,
                                     "OK",
                                     service.getSettings(request.getAppName(),
                                                         request.getEnvName(),
                                                         request.getSince()));
        } catch (Exception e) {
            log.error(String.format("fetch setting for {}-{} failed", request.getAppName(), request.getEnvName()), e);
            return new FetchResponse(500, e.getMessage(), null);
        }

    }
}
