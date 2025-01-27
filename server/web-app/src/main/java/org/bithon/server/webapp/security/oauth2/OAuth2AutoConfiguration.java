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

package org.bithon.server.webapp.security.oauth2;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.InvalidConfigurationException;
import org.bithon.server.web.service.security.IHttpSecurityCustomizer;
import org.bithon.server.web.service.security.jwt.JwtConfig;
import org.bithon.server.web.service.security.jwt.JwtTokenComponent;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 6/9/23 8:33 pm
 */
@Slf4j
@Configuration
public class OAuth2AutoConfiguration {

    @Data
    public static class OAuth2Config {
        private OAuth2ClientProperties client;
    }

    @Bean
    public IHttpSecurityCustomizer oauth2(Environment env,
                                          ApplicationContext applicationContext) {

        return httpSecurity -> {
            log.info("Configuring OAuth2 security for webapp");

            OAuth2Config oauth2Config = Binder.get(env)
                                              .bind("bithon.web.security.oauth2", OAuth2Config.class)
                                              .get();

            ServerProperties serverProperties = applicationContext.getBean(ServerProperties.class);
            String contextPath = StringUtils.hasText(serverProperties.getServlet().getContextPath()) ? serverProperties.getServlet().getContextPath() : "";

            JwtTokenComponent jwtTokenComponent = new JwtTokenComponent(applicationContext.getBean(JwtConfig.class));

            httpSecurity.sessionManagement((c) -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .csrf(AbstractHttpConfigurer::disable)
                        .authorizeHttpRequests(c -> c.requestMatchers(HttpMethod.GET, "/error").permitAll().anyRequest().authenticated())
                        //.addFilterBefore(new JwtAuthenticationFilter(jwtTokenComponent), OAuth2AuthorizationRequestRedirectFilter.class)
                        .oauth2Login(c -> c.clientRegistrationRepository(newClientRegistrationRepo(oauth2Config)).permitAll()
                                           .authorizationEndpoint(a -> a.authorizationRequestRepository(new HttpCookieOAuth2AuthorizationRequestRepository()))
                                           .successHandler(new OAuth2AuthSuccessHandler(jwtTokenComponent)))
                        .logout(c -> c.logoutSuccessUrl("/"))
                        .exceptionHandling(c -> c.authenticationEntryPoint(new OAuth2LoginAuthenticationEntryPoint(contextPath + "/oauth2/authorization/google")));
        };
    }

    private ClientRegistrationRepository newClientRegistrationRepo(OAuth2Config oauth2Config) {
        if (oauth2Config.getClient() == null) {
            throw new InvalidConfigurationException("bithon.web.security.oauth2.client is not configured.");
        }
        if (CollectionUtils.isEmpty(oauth2Config.getClient().getRegistration())) {
            throw new InvalidConfigurationException("bithon.web.security.oauth2.client.registration is not configured.");
        }

        Map<String, ClientRegistration> registrationMap = new OAuth2ClientPropertiesMapper(oauth2Config.getClient()).asClientRegistrations();
        return new InMemoryClientRegistrationRepository(registrationMap);
    }
}
