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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.agent.controller.rbac.Operation;
import org.bithon.server.agent.controller.rbac.User;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 22/1/25 5:25 pm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RbacConfig {
    private List<User> users;

    public boolean isPermitted(Operation operation, String user, String application, String resourceName) {
        if (CollectionUtils.isEmpty(users)) {
            // No rules defined, deny all
            return false;
        }

        // Find rules of given user
        Stream<User> userRules = users.stream().filter((rbacUser) -> rbacUser.getName().equals(user));

        return userRules.anyMatch((rbacUser) -> rbacUser.getPermissions()
                                                        .stream()
                                                        .anyMatch((permission) -> permission.isPermitted(operation, application, resourceName)));
    }
}
