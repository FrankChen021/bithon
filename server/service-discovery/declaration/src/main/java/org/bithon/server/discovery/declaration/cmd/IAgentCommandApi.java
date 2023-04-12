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
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

/**
 * @author Frank Chen
 * @date 2022/8/7 20:46
 */
@DiscoverableService(name = "agentCommand")
public interface IAgentCommandApi {

    /**
     * Declare all fields as public to treat it as a record
     * This simplifies Calcite related type construction.
     */
    @Data
    class AgentInstanceRecord {
        public String appName;
        public String agentId;
        public String endpoint;
        public String agentVersion;

        public Object[] toObjectArray() {
            return new Object[]{appName, agentId, endpoint, agentVersion};
        }
    }

    @GetMapping("/api/command/clients")
    ServiceResponse<AgentInstanceRecord> getAgentInstanceList();

    /**
     * Proxy Brpc services provided at agent side to allow them to be used over HTTP
     */
    @PostMapping("/api/command/proxy")
    byte[] proxy(@RequestParam(name = "agentId") String agentId,
                 @RequestParam(name = "token") String token,
                 @RequestBody byte[] body) throws IOException;
}
