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
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.collector.cmd.service.AgentCommandService;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
    public List<Map<String, String>> getClients() {
        return commandService.getServerChannel()
                             .getSessions()
                             .stream()
                             .map((session) -> {
                                 Map<String, String> client = new HashMap<>();
                                 client.put("appName", session.getAppName());
                                 if (StringUtils.hasText(session.getAppId())) {
                                     client.put("appId", session.getAppId());
                                 }
                                 client.put("endpoint", session.getEndpoint().toString());
                                 return client;
                             })
                             .sorted(Comparator.comparing(o -> o.get("appName")))
                             .collect(Collectors.toList());
    }

    @Override
    public List<String> dumpThread(@Valid @RequestBody CommandArgs<Void> args) {
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

            return Collections.singletonList(pw.toString());
        } catch (ServiceInvocationException e) {
            throw new RuntimeException(e.getMessage());
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
    public Collection<String> dumpClazz(@Valid @RequestBody CommandArgs<String> args) {
        IJvmCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IJvmCommand.class);
        if (command == null) {
            throw new RuntimeException(StringUtils.format("client by id [%s] not found", args.getAppId()));
        }
        try {
            String pattern;
            if (StringUtils.isEmpty(args.getArgs())) {
                pattern = ".*";
            } else {
                pattern = args.getArgs();
                pattern = pattern.replace(".", "\\.").replace("%", ".*");
            }

            return new TreeSet<>(command.dumpClazz(pattern));
        } catch (ServiceInvocationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Collection<String> getConfiguration(@RequestBody CommandArgs<GetConfigurationRequest> args) {
        IConfigCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IConfigCommand.class);
        if (command == null) {
            throw new RuntimeException(StringUtils.format("client by id [%s] not found", args.getAppId()));
        }
        try {
            GetConfigurationRequest request = args.getArgs();
            return Collections.singletonList(command.getConfiguration(request == null ? "YAML" : request.getFormat(),
                                                                      request == null ? true : request.isPretty()));
        } catch (ServiceInvocationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
