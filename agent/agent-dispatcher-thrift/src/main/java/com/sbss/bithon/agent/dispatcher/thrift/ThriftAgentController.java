/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.controller.IAgentController;
import com.sbss.bithon.agent.core.config.FetcherConfig;
import com.sbss.bithon.agent.rpc.thrift.service.setting.FetchRequest;
import com.sbss.bithon.agent.rpc.thrift.service.setting.FetchResponse;
import com.sbss.bithon.agent.rpc.thrift.service.setting.SettingService;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 4:01 下午
 */
public class ThriftAgentController implements IAgentController {

    private final AbstractThriftClient<SettingService.Client> client;

    public ThriftAgentController(FetcherConfig config) {
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
                    throw new RuntimeException(new TApplicationException("Server returns code="
                                                                         + response.getStatusCode()
                                                                         + ", Message="
                                                                         + response.getMessage()));
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
