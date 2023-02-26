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
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

/**
 * @author Frank Chen
 * @date 23/2/23 9:10 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentCommandDelegationApi {

    private final IAgentCommandApi impl;

    public AgentCommandDelegationApi(ServiceBroadcastInvoker serviceBroadcastInvoker) {
        this.impl = serviceBroadcastInvoker.create(IAgentCommandApi.class);
    }

    @GetMapping(value = "/api/agent/command/getClients"/*, produces = MediaType.TEXT_EVENT_STREAM_VALUE*/)
    public ServiceResponse getClients(HttpServletResponse httpResponse) {
        ServiceResponse response = impl.getClients();

        /*
        PrintWriter pw = httpResponse.getWriter();
        for (Object[] row : response.getData()) {
            pw.write((String) row[0]);
            pw.write('\n');
            pw.flush();
        }*/
        return response;
    }

    @GetMapping(value = "/api/agent/command/getClassList"/*, produces = MediaType.TEXT_EVENT_STREAM_VALUE*/)
    public ServiceResponse getClassList(@Valid @RequestBody CommandArgs<String> args, HttpServletResponse httpResponse) throws IOException {
        ServiceResponse response = impl.getClassList(args);

        /*
        int i = 0;
        PrintWriter pw = httpResponse.getWriter();
        for (Object[] row : response.getData()) {
            pw.write((String) row[0]);
            pw.write('\n');
            if (++i % 100 == 0) {
                pw.flush();
            }
        }*/
        return response;
    }

    @GetMapping(value = "/api/agent/command/getConfig"/*, produces = MediaType.TEXT_EVENT_STREAM_VALUE*/)
    public ServiceResponse getConfiguration(@Valid @RequestBody CommandArgs<IAgentCommandApi.GetConfigurationRequest> args,
                                            HttpServletResponse httpResponse) throws IOException {
        ServiceResponse response = impl.getConfiguration(args);

        /*
        PrintWriter pw = httpResponse.getWriter();
        for (Object[] row : response.getData()) {
            pw.write((String) row[0]);
            pw.write('\n');
            pw.flush();
        }*/
        return response;
    }

    @GetMapping(value = "/api/agent/command/getStackTrace"/*, produces = MediaType.TEXT_EVENT_STREAM_VALUE*/)
    public ServiceResponse getStackTrace(@Valid @RequestBody CommandArgs<Void> args,
                                         HttpServletResponse httpResponse) throws IOException {
        ServiceResponse response = impl.getStackTrace(args);

        /*
        // Output as stream
        StringWriter pw = new StringWriter();

        // Output header
        pw.write(StringUtils.format("---Total Threads: %d---\n", threads.size()));
        for (IJvmCommand.ThreadInfo thread : threads) {
            pw.write(StringUtils.format("Id: %d, Name: %s, State: %s \n", thread.getThreadId(), thread.getName(), thread.getState()));
            if (!thread.getStacks().isEmpty()) {
                String[] stackElements = thread.getStacks().split("\n");
                for (String stackElement : stackElements) {
                    pw.write('\t');
                    pw.write(stackElement);
                    pw.write('\n');
                }
            }
            pw.write('\n');
        }


        PrintWriter pw = response.getWriter();
        for (String configuration : configurations) {
            pw.write(configuration);
            pw.write('\n');
            pw.flush();
        }*/

        return response;
    }
}
