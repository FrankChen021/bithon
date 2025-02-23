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

import groovy.util.logging.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.web.security.cookie.CookieHelper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.time.Duration;

/**
 * @author Frank Chen
 * @date 8/9/23 3:28 pm
 */
@Slf4j
public class OAuth2LoginAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    public static final long MAX_AGE = Duration.ofDays(7).getSeconds();

    public static final String LOGIN_REDIRECT = "login_redirect";

    public OAuth2LoginAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    /**
     * To support customized login page, we need to configure the MvcConfigurer shown as below.
     * <pre>
     *      @Configuration
     *      public class MvcConfig implements WebMvcConfigurer {
     *          @Override
     *          public void addViewControllers(ViewControllerRegistry registry) {
     *              registry.addViewController("/login").setViewName("login");
     *          }
     *      }
     * </pre>
     */
    @Override
    protected String buildRedirectUrlToLoginPage(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 AuthenticationException authException) {

        String uri = request.getRequestURI();
        if (StringUtils.hasText(request.getQueryString())) {
            uri += "?" + request.getQueryString();
        }

        CookieHelper.Builder.newCookie(LOGIN_REDIRECT, uri)
                            .path("/")
                            .expiration(MAX_AGE)
                            .addTo(response);

        return super.buildRedirectUrlToLoginPage(request, response, authException);
    }
}
