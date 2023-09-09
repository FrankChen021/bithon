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
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author Frank Chen
 * @date 8/9/23 4:35 pm
 */
@Data
@Configuration
@ConfigurationProperties("bithon.web.security")
public class SecurityConfig {
    private boolean enabled = false;
    private String jwtTokenSignKey = "BithonIsAObservabilityPlatformThatMakesUEasy9";
    private long jwtTokenValiditySeconds = Duration.ofDays(1).getSeconds();

    private OAuth2Config oauth2;

    @Data
    public static class OAuth2Config {
        private OAuth2ClientProperties client;
    }
}
