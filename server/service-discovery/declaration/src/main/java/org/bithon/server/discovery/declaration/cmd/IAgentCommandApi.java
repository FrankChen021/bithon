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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    interface IObjectArrayConvertable {
        Object[] toObjectArray();
    }

    /**
     * Declare all fields as public to treat it as a record
     * This simplifies Calcite related type construction.
     */
    @Data
    class AgentInstanceRecord implements IObjectArrayConvertable {
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

    @Data
    class ThreadRecord implements IObjectArrayConvertable {
        public long threadId;
        public String name;
        public int daemon;
        public int priority;
        public String state;
        public long cpuTime;
        public long userTime;

        public long blockedTime;

        /**
         * The total number of times that the thread entered the BLOCKED state
         */
        public long blockedCount;

        /**
         * The approximate accumulated elapsed time in milliseconds that a thread has been in the WAITING or TIMED_WAITING state;
         * -1 if thread contention monitoring is disabled.
         */
        public long waitedTime;

        /**
         * The total number of times that the thread was in the WAITING or TIMED_WAITING state.
         */
        public long waitedCount;

        public String lockName;
        public long lockOwnerId;
        public String lockOwnerName;

        public int inNative;
        public int suspended;
        public String stack;

        public Object[] toObjectArray() {
            return new Object[]{
                    threadId,
                    name,
                    daemon,
                    priority,
                    state,
                    cpuTime,
                    userTime,
                    blockedTime,
                    blockedCount,
                    waitedTime,
                    waitedCount,
                    lockName,
                    lockOwnerId,
                    lockOwnerName,
                    inNative,
                    suspended,
                    stack
            };
        }
    }

    class ClassRecord implements IObjectArrayConvertable {
        public String name;
        public String classLoader;
        public int isSynthetic;
        public int isInterface;
        public int isAnnotation;
        public int isEnum;

        public Object[] toObjectArray() {
            return new Object[]{name, classLoader, isSynthetic, isInterface, isAnnotation, isEnum};
        }
    }

    @Data
    class GetConfigurationRequest {
        /**
         * JSON | YAML
         */
        private String format;
        private boolean pretty;
    }

    class ConfigurationRecord implements IObjectArrayConvertable {
        public String payload;

        public Object[] toObjectArray() {
            return new Object[]{payload};
        }
    }

    class InstrumentedMethodRecord implements IObjectArrayConvertable {
        public String interceptor;
        public String clazzName;
        public boolean isStatic;
        public String returnType;
        public String methodName;
        public String parameters;

        @Override
        public Object[] toObjectArray() {
            return new Object[]{interceptor, clazzName, isStatic, returnType, methodName, parameters};
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    class LoggerConfigurationRecord implements IObjectArrayConvertable {

        public String name;

        public String level;

        public String effectiveLevel;

        @Override
        public Object[] toObjectArray() {
            return new Object[]{name, level, effectiveLevel};
        }
    }

    @PostMapping("/api/command/config/get")
    ServiceResponse<ConfigurationRecord> getConfiguration(@RequestBody CommandArgs<GetConfigurationRequest> args);

    @PostMapping("/api/command/proxy")
    byte[] proxy(@RequestParam(name = "agentId") String agentId,
                 @RequestParam(name = "token") String token,
                 @RequestBody byte[] body) throws IOException;
}
