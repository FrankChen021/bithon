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

import org.bithon.agent.rpc.brpc.cmd.IConfigCommand;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.brpc.channel.ServerChannel;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.collector.cmd.service.AgentCommandService;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.CommandResponse;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
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
public class AgentCommandApi implements IAgentCommandApi {
    private final AgentCommandService commandService;

    public AgentCommandApi(AgentCommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public Map<String, List<Map<String, String>>> getClients() {
        Map<String, List<Map<String, String>>> clients = new HashMap<>(17);
        List<ServerChannel.Session> sessions = commandService.getServerChannel()
                                                             .getSessions();
        for (ServerChannel.Session session : sessions) {
            Map<String, String> properties = new HashMap<>();
            if (StringUtils.hasText(session.getAppId())) {
                properties.put("appId", session.getAppId());
            }
            properties.put("endpoint", session.getEndpoint().toString());

            clients.computeIfAbsent(session.getAppName(), v -> new ArrayList<>())
                   .add(properties);
        }
        return clients;
    }

    @Override
    public String dumpThread(@Valid @RequestBody CommandArgs<Void> args) {
        IJvmCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IJvmCommand.class);
        if (command == null) {
            throw new RuntimeException(StringUtils.format("client by id [%s] not found", args.getAppId()));
        }
        try {
            // Get
            List<IJvmCommand.ThreadInfo> threads = command.dumpThreads();

            // Sort by thread name for better analysis
            threads.sort(Comparator.comparing(IJvmCommand.ThreadInfo::getName));

            // Output as stream
            StringWriter pw = new StringWriter();

            // Output header
            pw.write(StringUtils.format("---Total Threads: %d---\n", threads.size()));
            for (IJvmCommand.ThreadInfo thread : threads) {
                pw.write(StringUtils.format("Id: %d, Name: %s, State: %s \n", thread.getThreadId(), thread.getName(), thread.getState()));
                String[] stackElements = thread.getStacks().split("\n");
                for (String stackElement : stackElements) {
                    pw.write('\t');
                    pw.write(stackElement);
                    pw.write('\n');
                }
                pw.write('\n');
            }

            return pw.toString();
        } catch (ServiceInvocationException e) {
            return e.getMessage();
        }
    }

    /**
     * @param args A string pattern which comply with database's like expression.
     *             For example:
     *             "%CommandApi" will match all classes whose name ends with CommandApi
     *             "CommandApi" matches only qualified class name that is the exact CommandApi
     *             "%bithon% matches all qualified classes whose name contains bithon
     */
    @Override
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

    @Override
    public ResponseEntity<String> getConfiguration(@RequestBody CommandArgs<GetConfigurationRequest> args) {
        IConfigCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IConfigCommand.class);
        if (command == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .header("Content-Type", "application/text")
                                 .body(StringUtils.format("client by id [%s] not found", args.getAppId()));
        }
        try {
            return ResponseEntity.status(HttpStatus.OK)
                                 .header("Content-Type", "application/text")
                                 .body(command.getConfiguration(args.getArgs().getFormat(), args.getArgs().isPretty()));
        } catch (ServiceInvocationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .header("Content-Type", "application/text")
                                 .body(e.getMessage());
        }
    }
}
