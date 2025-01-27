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

package org.bithon.server.discovery.declaration.controller;

import lombok.Data;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An API that proxies services running in agents over HTTP.
 *
 * @author Frank Chen
 * @date 2022/8/7 20:46
 */
@DiscoverableService(name = "agentCommand")
public interface IAgentControllerApi {
    /**
     * The two special parameters that will be extracted from the SQL and push down to the underlying query
     */
    String PARAMETER_NAME_APP_NAME = "appName";
    String PARAMETER_NAME_INSTANCE = "instance";

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
        public String buildId;
        public String buildTime;
        public LocalDateTime startAt;

        public Object[] toObjectArray() {
            return new Object[]{appName, instance, endpoint, controller, agentVersion, buildId, buildTime, startAt};
        }
    }

    /**
     * Get the information instances connected to one controller
     *
     * @param instance The specific instance that callers want to get information about
     */
    @GetMapping("/api/agent/service/instances")
    List<AgentInstanceRecord> getAgentInstanceList(@RequestParam(name = PARAMETER_NAME_APP_NAME, required = false) String application,
                                                   @RequestParam(name = PARAMETER_NAME_INSTANCE, required = false) String instance);

    /**
     * Call Brpc services provided at agent side over HTTP.
     *
     * @param instance the target client instance that the request will be sent to.
     * @param token    For WRITE operations (the method name does not start with 'get' or 'dump'), the token is required.
     * @param timeout  timeout value in milliseconds
     * @param message  message bytes serialized from ServiceRequestMessageOut
     */
    @PostMapping("/api/agent/service/proxy")
    byte[] callAgentService(@RequestHeader(name = "X-Bithon-Token", required = false) String token,
                            @RequestParam(name = PARAMETER_NAME_APP_NAME) String application,
                            @RequestParam(name = PARAMETER_NAME_INSTANCE) String instance,
                            @RequestParam(name = "timeout", required = false) Integer timeout,
                            @RequestBody byte[] message) throws IOException;

    @GetMapping("/api/agent/service/setting/update")
    void updateAgentSetting(@RequestParam(name = "appName") String appName,
                            @RequestParam(name = "env", required = false) String env);
}
