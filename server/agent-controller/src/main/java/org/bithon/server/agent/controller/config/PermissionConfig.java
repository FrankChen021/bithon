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

import lombok.Data;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.agent.controller.rbac.Operation;
import org.springframework.http.HttpStatus;

/**
 * A simple permission control on SET/WRITE commands to agent to ensure safety.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/8 15:08
 */
@Data
public class PermissionConfig {

    private boolean enabled;
    private RbacConfig rbac;

    public void verifyPermission(Operation operation,
                                 String user,
                                 String application,
                                 String resourceName) {

        if (rbac != null && !rbac.isPermitted(operation, user, application, resourceName)) {
            throw new HttpMappableException(HttpStatus.FORBIDDEN.value(),
                                            "No permission rule defined for [%s] to perform [%s] on resource [%s] in application [%s]. Contact the ADMIN to grant permission.",
                                            user,
                                            operation,
                                            resourceName,
                                            application);
        }
    }
}
