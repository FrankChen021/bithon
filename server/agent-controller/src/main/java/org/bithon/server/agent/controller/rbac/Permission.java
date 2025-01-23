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

package org.bithon.server.agent.controller.rbac;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author frank.chen021@outlook.com
 * @date 22/1/25 5:59 pm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    /**
     * when it's null, it defaults to RW
     */
    @Nullable
    private Operation operation;

    /**
     * specific application: app1
     * an application that starts with some text: app1*
     * an application that ends with some text: *app1
     */
    private String application;

    @Nullable
    private String resourceName;

    public boolean isPermitted(Operation operation, String application, String resourceName) {
        if (this.operation != null && !this.operation.isPermitted(operation)) {
            return false;
        }

        if (!matchesPattern(this.application, application)) {
            return false;
        }

        if (this.resourceName != null && !matchesPattern(this.resourceName, resourceName)) {
            return false;
        }

        return true;
    }

    private boolean matchesPattern(String pattern, String value) {
        if ("*".equals(pattern)) {
            return true;
        } else if (pattern.startsWith("*")) {
            return value.endsWith(pattern.substring(1));
        } else if (pattern.endsWith("*")) {
            return value.startsWith(pattern.substring(0, pattern.length() - 1));
        } else {
            return pattern.equals(value);
        }
    }
}
