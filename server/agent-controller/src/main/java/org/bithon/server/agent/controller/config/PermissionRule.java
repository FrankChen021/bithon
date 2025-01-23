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

package org.bithon.server.agent.controller.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.bithon.server.commons.matcher.IMatcher;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * application:   application = "bithon-server" &&
 * authorization: authorizations in ()
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/8 16:42
 */
@Data
public class PermissionRule {
    private Map<String, String> application;

    /**
     * Can be token or username
     */
    private Set<String> authorizations;

    /**
     * Because Spring Configuration doesn't support jackson's polymorphism,
     * we need to do it manually.
     * <p>
     * Also, because the configuration can be changed dynamically,
     * it's a little complex to cache the matcher object.
     * <p>
     * This matcher is used for an interactive query, it's not on a performance-critical path,
     * it's acceptable that the matcher object is instantiated every time.
     */
    public IMatcher getApplicationMatcher(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(application), IMatcher.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
