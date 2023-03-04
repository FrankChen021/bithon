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
    class InstanceRecord implements IObjectArrayConvertable {
        public String appName;
        public String appId;
        public String endpoint;

        public Object[] toObjectArray() {
            return new Object[]{appName, appId, endpoint};
        }
    }

    @GetMapping("/api/command/clients")
    ServiceResponse<InstanceRecord> getClients();

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

    @PostMapping("/api/command/jvm/dumpThread")
    ServiceResponse<ThreadRecord> getThreads(@RequestBody CommandArgs<Void> args);

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

    /**
     * Get loaded class
     */
    @PostMapping("/api/command/jvm/dumpClazz")
    ServiceResponse<ClassRecord> getClass(@RequestBody CommandArgs<Void> args);

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

    @PostMapping("/api/command/config/get")
    ServiceResponse<ConfigurationRecord> getConfiguration(@RequestBody CommandArgs<GetConfigurationRequest> args);
}
