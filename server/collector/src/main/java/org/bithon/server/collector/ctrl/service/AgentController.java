/*
 *    Copyright 2020 bithon.org
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

package org.bithon.server.collector.ctrl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.collector.ctrl.config.AgentControllerConfig;
import org.bithon.server.collector.source.brpc.BrpcCollectorServer;
import org.bithon.server.storage.setting.ISettingStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 7:37 下午
 */
@Service
@ConditionalOnProperty(value = "bithon.agent-controller.enabled", havingValue = "true")
public class AgentController {

    private final BrpcServer brpcServer;

    public AgentController(ObjectMapper jsonFormatter,
                           ISettingStorage storage,
                           Environment env,
                           BrpcCollectorServer server) {
        AgentControllerConfig config = Binder.get(env).bind("bithon.agent-controller", AgentControllerConfig.class).get();
        Preconditions.checkIfTrue(config.getPort() > 1000 && config.getPort() < 65535, "The port of bithon.agent-controller property must be in the range of [1000, 65535)");

        brpcServer = server.addService("ctrl",
                                       new AgentSettingFetcher(storage.createReader(), jsonFormatter),
                                       config.getPort())
                           .getBrpcServer();
    }

    public BrpcServer getBrpcServer() {
        return this.brpcServer;
    }
}