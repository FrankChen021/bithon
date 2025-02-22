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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.web.security.cookie.CookieHelper;
import org.bithon.server.web.security.jwt.JwtTokenComponent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Issue JWT token and redirect to the page that launches the login
 *
 * @author Frank Chen
 * @date 8/9/23 5:08 pm
 */
public class OAuth2AuthSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenComponent jwtTokenComponent;

    public OAuth2AuthSuccessHandler(JwtTokenComponent jwtTokenComponent) {
        this.jwtTokenComponent = jwtTokenComponent;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // Keep the ROLE_USER only
        OAuth2UserAuthority user = (OAuth2UserAuthority) oauthUser.getAuthorities()
                                                                  .stream()
                                                                  .filter((auth) -> "OAUTH2_USER".equals(auth.getAuthority()))
                                                                  .findFirst()
                                                                  .get();

        // Keep email and name only
        String email = (String) user.getAttributes().get("email");
        OAuth2UserAuthority newAuthority = new OAuth2UserAuthority(Map.of("name", user.getAttributes().get("name")));

        String newJwtToken = jwtTokenComponent.createToken(email, Collections.singletonList(newAuthority));

        // Store the JWT in a cookie
        CookieHelper.Builder.newCookie(JwtTokenComponent.COOKIE_NAME_TOKEN, newJwtToken)
                            .expiration(jwtTokenComponent.getGlobalValidityMilliseconds() / 1000)
                            .path("/")
                            .addTo(response);

        // Redirect based on the value stored in cookie
        String redirect = CookieHelper.get(request, OAuth2LoginAuthenticationEntryPoint.LOGIN_REDIRECT);
        if (StringUtils.isEmpty(redirect)) {
            redirect = "/web/home";
        }
        CookieHelper.delete(request, response, OAuth2LoginAuthenticationEntryPoint.LOGIN_REDIRECT);
        response.sendRedirect(redirect);
    }
}
