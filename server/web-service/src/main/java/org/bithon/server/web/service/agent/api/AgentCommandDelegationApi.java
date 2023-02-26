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

package org.bithon.server.web.service.agent.api;

import org.bithon.component.commons.utils.StringUtils;
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
import java.io.PrintWriter;

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

    @GetMapping(value = "/api/agent/command/getClients")
    public ServiceResponse<IAgentCommandApi.Client> getClients() {
        return impl.getClients();
    }

    @GetMapping(value = "/api/agent/command/getClassList", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void getClassList(@Valid @RequestBody CommandArgs<String> args, HttpServletResponse httpResponse) throws IOException {
        ServiceResponse<String> response = impl.getClassList(args);

        if (response.getError() != null) {
            printError(httpResponse, response.getError());
            return;
        }

        int i = 0;
        PrintWriter pw = httpResponse.getWriter();
        for (String row : response.getRows()) {
            pw.write(row);
            pw.write('\n');
            if (++i % 100 == 0) {
                pw.flush();
            }
        }
    }

    @GetMapping(value = "/api/agent/command/getConfig", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void getConfiguration(@Valid @RequestBody CommandArgs<IAgentCommandApi.GetConfigurationRequest> args,
                                 HttpServletResponse httpResponse) throws IOException {
        ServiceResponse<String> response = impl.getConfiguration(args);

        if (response.getError() != null) {
            printError(httpResponse, response.getError());
            return;
        }

        PrintWriter pw = httpResponse.getWriter();
        for (String row : response.getRows()) {
            pw.write(row);
            pw.write('\n');
            pw.flush();
        }
    }

    @GetMapping(value = "/api/agent/command/getStackTrace", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void getStackTrace(@Valid @RequestBody CommandArgs<Void> args,
                              HttpServletResponse httpResponse) throws IOException {
        ServiceResponse<IAgentCommandApi.StackTrace> response = impl.getStackTrace(args);

        if (response.getError() != null) {
            printError(httpResponse, response.getError());
            return;
        }

        // Output as stream
        PrintWriter pw = httpResponse.getWriter();

        // Output header
        pw.write(StringUtils.format("---Total Threads: %d---\n", response.getRows().size()));
        for (IAgentCommandApi.StackTrace stackTrace : response.getRows()) {
            pw.write(StringUtils.format("Id: %d, Name: %s, State: %s \n", stackTrace.getThreadId(), stackTrace.getName(), stackTrace.getState()));
            if (!stackTrace.getStack().isEmpty()) {
                String[] stackElements = stackTrace.getStack().split("\n");
                for (String stackElement : stackElements) {
                    pw.write('\t');
                    pw.write(stackElement);
                    pw.write('\n');
                }
            }
            pw.write('\n');
        }
    }

    private void printError(HttpServletResponse httpResponse, ServiceResponse.Error error) throws IOException {
        PrintWriter pw = httpResponse.getWriter();

        pw.write(StringUtils.format("uri: %s\n", error.getUri()));
        pw.write(StringUtils.format("exception: %s\n", error.getException()));
        pw.write(StringUtils.format("message: %s\n", error.getMessage()));
    }
}
