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

package org.bithon.server.webapp.security;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.Collections;


/**
 * @author Frank Chen
 * @date 25/9/23 2:45 pm
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(value = "bithon.web.security.enabled", havingValue = "true")
public class SecurityApi {

    private final WebSecurityConfig securityConfig;

    public SecurityApi(WebSecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Data
    public static class CreateTokenRequest {
        @NotEmpty
        // The username that the token is issue to.
        // Currently, it's the email
        private String issueTo;

        @Min(1)
        // in seconds
        private long validitySeconds;
    }

    @PostMapping("/api/security/createToken")
    public String createToken(@Validated @RequestBody CreateTokenRequest request) {
        String currentUser = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return new JwtTokenComponent(securityConfig).createToken(request.issueTo,
                                                                 Collections.emptyList(),
                                                                 currentUser,
                                                                 request.validitySeconds * 1000L);
    }
}
