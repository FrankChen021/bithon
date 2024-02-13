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

package org.bithon.server.alerting.manager.security;

import org.bithon.server.alerting.manager.biz.BizException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Frank Chen
 * @date 12/11/21 2:58 pm
 */
public class DefaultUserProvider implements IUserProvider {

    private final boolean allowAnonymous;

    public DefaultUserProvider(boolean allowAnonymous) {
        this.allowAnonymous = allowAnonymous;
    }

    @Override
    public User getCurrentUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext == null) {
            throw new BizException("No authenticated user.");
        }
        Authentication auth = securityContext.getAuthentication();
        if (!allowAnonymous && (auth == null || auth instanceof AnonymousAuthenticationToken)) {
            throw new BizException("No authenticated user.");
        }

        return new User((String) auth.getPrincipal());
    }
}
