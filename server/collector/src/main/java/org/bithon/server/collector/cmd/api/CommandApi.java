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

package org.bithon.server.collector.cmd.api;

import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.collector.cmd.service.CommandService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * @author Frank Chen
 * @date 2022/8/7 20:46
 */
@RestController
public class CommandApi {
    private final CommandService commandService;

    public CommandApi(CommandService commandService) {
        this.commandService = commandService;
    }

    @GetMapping("/api/command/client")
    public Set<EndPoint> getClients() {
        return commandService.getServerChannel().getClientEndpoints();
    }

    @PostMapping("/api/command/jvm/dumpThread")
    public CommandResponse<List<IJvmCommand.ThreadInfo>> dumpThread(@Valid @RequestBody CommandArgs<Void> args) {
        ClientApplication client = args.getClient();
        IJvmCommand command = commandService.getServerChannel().getRemoteService(new EndPoint(client.getHost(), client.getPort()), IJvmCommand.class);
        if (command == null) {
            return CommandResponse.error(StringUtils.format("client[%s:%d] not found", client.getHost(), client.getPort()));
        }
        try {
            return CommandResponse.success(command.dumpThreads());
        } catch (ServiceInvocationException e) {
            return CommandResponse.exception(e);
        }
    }
}
