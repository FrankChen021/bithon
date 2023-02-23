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

package org.bithon.server.web.service.agent.api.impl;

import org.bithon.server.discovery.client.ServiceBroadcastInvoker;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.CommandResponse;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Frank Chen
 * @date 23/2/23 9:10 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentCommandDelegationApi {

    private final ServiceBroadcastInvoker serviceBroadcastInvoker;

    public AgentCommandDelegationApi(ServiceBroadcastInvoker serviceBroadcastInvoker) {
        this.serviceBroadcastInvoker = serviceBroadcastInvoker;
    }

    @GetMapping("/api/agent/command/getClients")
    public Map<String, List<Map<String, String>>> getClients() {
        IAgentCommandApi impl = serviceBroadcastInvoker.create(IAgentCommandApi.class);
        return impl.getClients();
    }

    @PostMapping("/api/agent/command/dumpClazz")
    public CommandResponse<Set<String>> dumpClazz(@RequestBody CommandArgs<String> args) {
        IAgentCommandApi impl = serviceBroadcastInvoker.create(IAgentCommandApi.class);
        return impl.dumpClazz(args);
    }
}
