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

import org.bithon.server.webapp.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
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

/**
 * @author Frank Chen
 * @date 6/9/23 8:33 pm
 */
@Configuration
@EnableWebSecurity
public class SecurityConfigurer extends WebSecurityConfigurerAdapter {

    private final SecurityConfig securityConfig;

    public SecurityConfigurer(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public void configure(WebSecurity web) {
        // Configure to ignore security check on static resources
        web.ignoring()
           .antMatchers("/images/**", "/css/**", "/lib/**", "/js/**")
           .antMatchers("/login");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (!securityConfig.isEnabled()) {
            // Permit all
            http.csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/**").permitAll();
            return;
        }

        JwtTokenComponent jwtTokenComponent = new JwtTokenComponent(securityConfig);

        http.csrf()
            .disable()
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
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .logout().logoutSuccessUrl("/")
            .and()
            .exceptionHandling().authenticationEntryPoint(new LoginAuthenticationEntryPoint("/oauth2/authorization/google"))
        ;
    }

    private ClientRegistrationRepository newClientRegistrationRepo() {
        Map<String, ClientRegistration> registrationMap = OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(securityConfig.getOauth2().getClient());
        return new InMemoryClientRegistrationRepository(registrationMap);
    }
}