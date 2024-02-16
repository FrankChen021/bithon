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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * An API that proxies services running in agents over HTTP.
 *
 * @author Frank Chen
 * @date 2022/8/7 20:46
 */
@DiscoverableService(name = "agentCommand")
public interface IAgentProxyApi {

    /**
     * Declare all fields as public to treat it as a record
     * This simplifies Calcite related type construction.
     */
    @Data
    class AgentInstanceRecord {
        public String appName;
        public String instance;
        public String endpoint;
        public String controller;
        public String agentVersion;
        public LocalDateTime startAt;

        public Object[] toObjectArray() {
            return new Object[]{appName, instance, endpoint, controller, agentVersion, startAt};
        }
    }

    @GetMapping("/api/agent/service/instances")
    ServiceResponse<AgentInstanceRecord> getAgentInstanceList();

    String INSTANCE_FIELD = "instance";

    /**
     * Proxy Brpc services provided at agent side over HTTP.
     *
     * @param instance          the target client instance that the request will be sent to.
     * @param token             For WRITE operations (the method name does not start with 'get' or 'dump'), the token is required.
     * @param timeout           timeout value in milliseconds
     */
    @PostMapping("/api/agent/service/proxy")
    byte[] proxy(@RequestHeader(name = "token", required = false) String token,
                 @RequestParam(name = INSTANCE_FIELD) String instance,
                 @RequestParam(name = "timeout", required = false) Integer timeout,
                 @RequestBody byte[] rawMessage) throws IOException;
}
