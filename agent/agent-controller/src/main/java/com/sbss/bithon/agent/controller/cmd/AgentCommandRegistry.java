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

package com.sbss.bithon.agent.controller.cmd;

import shaded.com.fasterxml.jackson.annotation.JsonInclude;
import shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentCommandRegistry {
    private static final Logger log = LoggerFactory.getLogger(AgentCommandRegistry.class);

    private static final Map<String, IAgentCommand<?>> COMMANDS = new ConcurrentHashMap<>();

    public static void registerCommand(String name, IAgentCommand<?> command) {
        COMMANDS.putIfAbsent(name, command);
    }

    public static IAgentCommand<?> getCommand(String commandName) {
        return COMMANDS.get(commandName);
    }

    public static void dispatch(String commandName, String token, String jsonArgument) {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        AgentCommandResponse<?> response;
        try {
            if (!"-sentinel-admin-token-".equals(token)) {
                throw new NotAuthorizedException();
            }

            IAgentCommand<?> command = getCommand(commandName);
            if (command == null) {
                throw new NoSuchCommandException(commandName);
            }

            //log.info("Executing Agent Command[{}]: {}", commandName, body);
            Object inputArgs = om.readValue(jsonArgument, command.getRequestType());
            response = command.execute(inputArgs);
        } catch (AgentCommandException e) {
            response = e.getResponse();
        } catch (Exception e) {
            StringWriter sw = new StringWriter(256);
            e.printStackTrace(new PrintWriter(sw));
            response = AgentCommandResponse.fail(500, sw.toString());
        }
    }
}
