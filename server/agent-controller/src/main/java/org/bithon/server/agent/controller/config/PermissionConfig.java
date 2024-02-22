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
import org.bithon.component.commons.exception.HttpMappableException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple permission control on SET/WRITE commands to agent to ensure safety.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/8 15:08
 */
@Data
public class PermissionConfig {

    private List<PermissionRule> rules = Collections.emptyList();

    public void verifyPermission(ObjectMapper objectMapper, String application, String authorization) {
        List<PermissionRule> applicationRules = this.rules.stream()
                                                          .filter((rule) -> rule.getApplicationMatcher(objectMapper).matches(application))
                                                          .collect(Collectors.toList());
        if (applicationRules.isEmpty()) {
            throw new HttpMappableException(HttpStatus.FORBIDDEN.value(),
                                            "No permission rule is defined for application [%s].",
                                            application);
        }

        boolean permitted = applicationRules.stream().anyMatch((rule) -> rule.getAuthorizations() != null && rule.getAuthorizations().contains(authorization));
        if (!permitted) {
            throw new HttpMappableException(HttpStatus.FORBIDDEN.value(),
                                            "Given authorization [%s] does not match existing permission rule for WRITE operation on application [%s]",
                                            authorization,
                                            application);
        }
    }
}
