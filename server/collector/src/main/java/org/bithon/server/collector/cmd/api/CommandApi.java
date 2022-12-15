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

import com.google.common.collect.ImmutableMap;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.brpc.channel.ServerChannel;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.collector.cmd.service.CommandService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    @GetMapping("/api/command/clients")
    public Map<String, List<Map<String, String>>> getClients() {
        Map<String, List<Map<String, String>>> clients = new HashMap<>(17);
        List<ServerChannel.Session> sessions = commandService.getServerChannel()
                                                             .getSessions();
        for (ServerChannel.Session session : sessions) {
            clients.computeIfAbsent(session.getAppName(), v -> new ArrayList<>(4))
                   .add(ImmutableMap.of("appId", session.getAppId()));
        }
        return clients;
    }

    @PostMapping("/api/command/jvm/dumpThread")
    public CommandResponse<List<IJvmCommand.ThreadInfo>> dumpThread(@Valid @RequestBody CommandArgs<Void> args) {
        IJvmCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IJvmCommand.class);
        if (command == null) {
            return CommandResponse.error(StringUtils.format("client by id [%s] not found", args.getAppId()));
        }
        try {
            return CommandResponse.success(command.dumpThreads());
        } catch (ServiceInvocationException e) {
            return CommandResponse.exception(e);
        }
    }

    /**
     * @param args A string pattern which comply with database's like expression.
     *             For example:
     *             "%CommandApi" will match all classes whose name ends with CommandApi
     *             "CommandApi" matches only qualified class name that is the exact CommandApi
     *             "%bithon% matches all qualified classes whose name contains bithon
     */
    @PostMapping("/api/command/jvm/dumpClazz")
    public CommandResponse<Set<String>> dumpClazz(@Valid @RequestBody CommandArgs<String> args) {
        IJvmCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IJvmCommand.class);
        if (command == null) {
            return CommandResponse.error(StringUtils.format("client by id [%s] not found", args.getAppId()));
        }
        try {
            String pattern;
            if (StringUtils.isEmpty(args.getArgs())) {
                pattern = ".*";
            } else {
                pattern = args.getArgs();
                pattern = pattern.replace(".", "\\.").replace("%", ".*");
            }

            return CommandResponse.success(new TreeSet<>(command.dumpClazz(pattern)));
        } catch (ServiceInvocationException e) {
            return CommandResponse.exception(e);
        }
    }
}
