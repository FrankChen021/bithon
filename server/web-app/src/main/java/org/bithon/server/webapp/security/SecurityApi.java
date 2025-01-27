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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.security.JwtConfig;
import org.bithon.server.commons.security.JwtTokenComponent;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.commons.utils.HumanReadableDurationConstraint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;


/**
 * @author Frank Chen
 * @date 25/9/23 2:45 pm
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(value = "bithon.web.security.enabled", havingValue = "true")
public class SecurityApi {

    private final JwtConfig jwtConfig;

    public SecurityApi(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Data
    public static class CreateTokenRequest {
        @NotEmpty
        // The username that the token is issue to.
        // Currently, it's the email
        private String issueTo;

        @HumanReadableDurationConstraint(min = "1m")
        private HumanReadableDuration validity;
    }

    @PostMapping("/api/security/token/create")
    public String createToken(@Validated @RequestBody CreateTokenRequest request) {
        String currentUser = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return new JwtTokenComponent(jwtConfig).createToken(request.issueTo,
                                                            Collections.emptyList(),
                                                            currentUser,
                                                            request.validity.getDuration());
    }

    @Data
    public static class GetTokenValidityRequest {
        private String token;
    }

    @Data
    @Builder
    public static class GetTokenValidityResponse {
        private String expiredAt;
        private String error;
    }

    @PostMapping("/api/security/token/validity")
    public GetTokenValidityResponse getTokenValidity(@RequestBody GetTokenValidityRequest request) {
        String token;
        if (StringUtils.isBlank(request.getToken())) {
            // check current token
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            token = (String) auth.getCredentials();
        } else {
            token = request.getToken();
        }

        try {
            JwtTokenComponent tokenComponent = new JwtTokenComponent(jwtConfig);
            Jws<Claims> parsedToken = tokenComponent.parseToken(token);
            return GetTokenValidityResponse.builder()
                                           .expiredAt(TimeSpan.of(tokenComponent.getExpirationTimestamp(parsedToken)).toISO8601())
                                           .build();
        } catch (ExpiredJwtException ignored) {
            return GetTokenValidityResponse.builder()
                                           .error("Given token expired")
                                           .build();
        } catch (Exception e) {
            return GetTokenValidityResponse.builder()
                                           .error("Failed to decode token: " + e.getMessage())
                                           .build();
        }
    }
}
