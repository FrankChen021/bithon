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
import java.util.Collection;
import java.util.Map;

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

    @GetMapping("/api/agent/command/getClients")
    public Collection<Map<String, String>> getClients() {
        return impl.getClients();
    }

    @GetMapping(value = "/api/agent/command/getClassList", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void getClassList(@Valid @RequestBody CommandArgs<String> args, HttpServletResponse response) throws IOException {
        Collection<String> classList = impl.getClassList(args);

        int i = 0;
        PrintWriter pw = response.getWriter();
        for (String clazz : classList) {
            pw.write(clazz);
            pw.write('\n');
            if (++i % 100 == 0) {
                pw.flush();
            }
        }
    }

    @GetMapping(value = "/api/agent/command/getConfig", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void getConfiguration(@Valid @RequestBody CommandArgs<IAgentCommandApi.GetConfigurationRequest> args,
                                 HttpServletResponse response) throws IOException {
        Collection<String> configurations = impl.getConfiguration(args);

        PrintWriter pw = response.getWriter();
        for (String configuration : configurations) {
            pw.write(configuration);
            pw.write('\n');
            pw.flush();
        }
    }

    @GetMapping(value = "/api/agent/command/getStackTrace", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void getStackTrace(@Valid @RequestBody CommandArgs<Void> args,
                              HttpServletResponse response) throws IOException {
        Collection<String> configurations = impl.getThreadStackTrace(args);

        PrintWriter pw = response.getWriter();
        for (String configuration : configurations) {
            pw.write(configuration);
            pw.write('\n');
            pw.flush();
        }
    }
}
