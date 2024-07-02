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

import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.InvalidConfigurationException;
import org.bithon.server.webapp.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Frank Chen
 * @date 6/9/23 8:33 pm
 */
@Configuration
public class WebSecurityConfigurer {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(ServerProperties serverProperties) {
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

        // Configure to ignore security check on static resources
        return (web) -> web.ignoring().requestMatchers(ignoreList);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ServerProperties serverProperties,
                                           WebSecurityConfig securityConfig) throws Exception {
        String contextPath = StringUtils.hasText(serverProperties.getServlet().getContextPath()) ? serverProperties.getServlet().getContextPath() : "";

        // H2 web UI requires disabling frameOptions.
        // This is not a graceful way. The better way is to check whether the H2 web UI is enabled in this module.
        // For simplicity, we just disable the frame option in global.
        http.headers((c) -> c.frameOptions((HeadersConfigurer.FrameOptionsConfig::disable)));

        if (!securityConfig.isEnabled()) {
            // Permit all
            return http.csrf(c -> {
                try {
                    c.disable().authorizeHttpRequests(r -> r.anyRequest().permitAll());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).build();
        }

        JwtTokenComponent jwtTokenComponent = new JwtTokenComponent(securityConfig);

        http.sessionManagement((c) -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(c -> c.requestMatchers(HttpMethod.GET, "/error").permitAll().anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenComponent), OAuth2AuthorizationRequestRedirectFilter.class)
            .oauth2Login(c -> c.clientRegistrationRepository(newClientRegistrationRepo(securityConfig)).permitAll()
                               .authorizationEndpoint(a -> a.authorizationRequestRepository(new HttpCookieOAuth2AuthorizationRequestRepository()))
                               .successHandler(new AuthSuccessHandler(jwtTokenComponent, securityConfig)))
            .logout(c -> c.logoutSuccessUrl("/"))
            .exceptionHandling(c -> c.authenticationEntryPoint(new LoginAuthenticationEntryPoint(contextPath + "/oauth2/authorization/google")));

        return http.build();
    }

    private ClientRegistrationRepository newClientRegistrationRepo(WebSecurityConfig securityConfig) {
        if (securityConfig.getOauth2() == null) {
            throw new InvalidConfigurationException("bithon.web.security.oauth2 is not configured.");
        }
        if (securityConfig.getOauth2().getClient() == null) {
            throw new InvalidConfigurationException("bithon.web.security.oauth2.client is not configured.");
        }
        if (CollectionUtils.isEmpty(securityConfig.getOauth2().getClient().getRegistration())) {
            throw new InvalidConfigurationException("bithon.web.security.oauth2.client.registration is not configured.");
        }

        Map<String, ClientRegistration> registrationMap = new OAuth2ClientPropertiesMapper(securityConfig.getOauth2().getClient()).asClientRegistrations();
        return new InMemoryClientRegistrationRepository(registrationMap);
    }
}
