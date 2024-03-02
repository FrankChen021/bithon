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
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;

import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Frank Chen
 * @date 6/9/23 8:33 pm
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {

    private final WebSecurityConfig securityConfig;
    private final String contextPath;

    public WebSecurityConfigurer(ServerProperties serverProperties, WebSecurityConfig securityConfig) {
        String contextPath = serverProperties.getServlet().getContextPath();
        this.contextPath = StringUtils.hasText(contextPath) ? contextPath : "";
        this.securityConfig = securityConfig;
    }

    @Override
    public void configure(WebSecurity web) {
        Stream<String> ignoreList = Stream.of(
            // Web application static resources
            "/images/**",
            "/css/**",
            "/lib/**",
            "/js/**",
            "/login",
            // Actuator for health check
            "/actuator/**",
            // Other APIs that do not require authentication
            "/api/security/token/validity");

        if (StringUtils.hasText(this.contextPath)) {
            ignoreList = ignoreList.map((path) -> this.contextPath + path);
        }

        // Configure to ignore security check on static resources
        web.ignoring()
           .antMatchers(ignoreList.toArray(String[]::new));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // H2 web UI requires disabling frameOptions.
        // This is not a graceful way. The better way is to check whether the H2 web UI is enabled in this module.
        // For simplicity, we just disable the frame option in global.
        http.headers()
            .frameOptions()
            .disable();

        if (!securityConfig.isEnabled()) {
            // Permit all
            http.csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/**")
                .permitAll();
            return;
        }

        JwtTokenComponent jwtTokenComponent = new JwtTokenComponent(securityConfig);

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/error").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenComponent), OAuth2AuthorizationRequestRedirectFilter.class)
            .oauth2Login()
            .clientRegistrationRepository(newClientRegistrationRepo()).permitAll()
            .authorizationEndpoint().authorizationRequestRepository(new HttpCookieOAuth2AuthorizationRequestRepository())
            .and()
            .successHandler(new AuthSuccessHandler(jwtTokenComponent, securityConfig))
            .and()
            .logout().logoutSuccessUrl("/")
            .and()
            .exceptionHandling().authenticationEntryPoint(new LoginAuthenticationEntryPoint(this.contextPath + "/oauth2/authorization/google"));
    }

    private ClientRegistrationRepository newClientRegistrationRepo() {
        if (securityConfig.getOauth2() == null) {
            throw new InvalidConfigurationException("bithon.web.security.oauth2 is not configured.");
        }
        if (securityConfig.getOauth2().getClient() == null) {
            throw new InvalidConfigurationException("bithon.web.security.oauth2.client is not configured.");
        }
        if (CollectionUtils.isEmpty(securityConfig.getOauth2().getClient().getRegistration())) {
            throw new InvalidConfigurationException("bithon.web.security.oauth2.client.registration is not configured.");
        }

        Map<String, ClientRegistration> registrationMap = OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(securityConfig.getOauth2().getClient());
        return new InMemoryClientRegistrationRepository(registrationMap);
    }
}
