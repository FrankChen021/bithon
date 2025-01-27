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

package org.bithon.server.web.service.security;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.web.service.security.filter.JwtAuthenticationFilter;
import org.bithon.server.web.service.security.jwt.JwtConfig;
import org.bithon.server.web.service.security.jwt.JwtTokenComponent;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Frank Chen
 * @date 6/9/23 8:33 pm
 */
@Slf4j
@Configuration
public class SecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity,
                                           ApplicationContext applicationContext) throws Exception {
        boolean isSecurityEnabled = Binder.get(applicationContext.getEnvironment())
                                          .bind("bithon.web.security.enabled", Boolean.class)
                                          .orElse(false);

        ServerProperties serverProperties = applicationContext.getBean(ServerProperties.class);
        String contextPath = StringUtils.hasText(serverProperties.getServlet().getContextPath()) ? serverProperties.getServlet().getContextPath() : "";

        String[] ignoreList = Stream.of("/images/**",
                                        "/css/**",
                                        "/lib/**",
                                        "/js/**",
                                        "/login",
                                        "/actuator/**"
                                    )
                                    .map((path) -> contextPath + path)
                                    .toArray(String[]::new);
        httpSecurity.authorizeHttpRequests((c) -> c.requestMatchers(ignoreList).permitAll());

        // H2 web UI requires disabling frameOptions.
        // This is not a graceful way. The better way is to check whether the H2 web UI is enabled in this module.
        // For simplicity, we just disable the frame option in global.
        httpSecurity.headers((c) -> c.frameOptions((HeadersConfigurer.FrameOptionsConfig::disable)));

        if (!isSecurityEnabled) {
            // Permit all
            return httpSecurity.csrf(c -> {
                try {
                    c.disable().authorizeHttpRequests(r -> r.anyRequest().permitAll());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).build();
        }

        log.info("Configuring JWT Security");
        httpSecurity.addFilterBefore(new JwtAuthenticationFilter(new JwtTokenComponent(applicationContext.getBean(JwtConfig.class))),
                                     OAuth2AuthorizationRequestRedirectFilter.class);

        // Get all beans that implement IHttpSecurityCustomizer and call their customize method
        Map<String, IHttpSecurityCustomizer> customizers = applicationContext.getBeansOfType(IHttpSecurityCustomizer.class);
        for (IHttpSecurityCustomizer customizer : customizers.values()) {
            customizer.customize(httpSecurity);
        }

        return httpSecurity.build();
    }
}
