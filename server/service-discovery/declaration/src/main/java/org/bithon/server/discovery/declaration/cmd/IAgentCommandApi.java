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

package org.bithon.server.discovery.declaration.cmd;

import lombok.Data;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

/**
 * @author Frank Chen
 * @date 2022/8/7 20:46
 */
@DiscoverableService(name = "agentCommand")
public interface IAgentCommandApi {

    @Data
    class Client {
        private String appName;
        private String appId;
        private String endpoint;
    }

    @GetMapping("/api/command/clients")
    ServiceResponse<Client> getClients();

    @Data
    class StackTrace {
        private String name;
        private long threadId;
        private boolean isDaemon;
        private int priority;
        private String state;
        private long cpuTime;
        private long userTime;
        private String stack;
    }

    @PostMapping("/api/command/jvm/dumpThread")
    ServiceResponse<StackTrace> getStackTrace(@RequestBody CommandArgs<Void> args) throws IOException;

    /**
     * @param args A string pattern which comply with database's like expression.
     *             For example:
     *             "%CommandApi" will match all classes whose name ends with CommandApi
     *             "CommandApi" matches only qualified class name that is the exact CommandApi
     *             "%bithon% matches all qualified classes whose name contains bithon
     */
    @PostMapping("/api/command/jvm/dumpClazz")
    ServiceResponse<String> getClassList(@RequestBody CommandArgs<String> args);

    @Data
    class GetConfigurationRequest {
        /**
         * JSON | YAML
         */
        private String format;
        private boolean pretty;
    }

    @PostMapping("/api/command/config/get")
    ServiceResponse<String> getConfiguration(@RequestBody CommandArgs<GetConfigurationRequest> args);
}
