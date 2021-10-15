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

package org.bithon.server.cmd.api;

import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.server.cmd.CommandService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 5:22 下午
 */
@CrossOrigin
@RestController
public class CommandApi {

    private final CommandService commandService;

    public CommandApi(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/api/cmd/getEndpointList")
    public Set<EndPoint> getAllEndpointList() {
        return commandService.getServerChannel().getClientEndpoints();
    }

    @PostMapping("/api/cmd/dumpThread")
    public List<IJvmCommand.ThreadInfo> dumpThread(@RequestBody InstanceCommandRequest req) {
        IJvmCommand jvmCommand = commandService.getServerChannel()
                                               .getRemoteService(new EndPoint(req.getInstanceIp(),
                                                                              req.getInstancePort()),
                                                                 IJvmCommand.class);
        if (jvmCommand == null) {
            return Collections.emptyList();
        }

        return jvmCommand.dumpThreads();
    }

}
