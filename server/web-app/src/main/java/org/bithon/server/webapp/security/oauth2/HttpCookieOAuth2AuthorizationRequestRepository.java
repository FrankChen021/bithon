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
import org.bithon.server.webapp.security.CookieHelper;
import org.bithon.server.webapp.security.LoginAuthenticationEntryPoint;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

/**
 * Use to replace the default HTTP session-based request repository to achieve stateless processing
 *
 * @author Frank Chen
 * @date 7/9/23 11:57 pm
 */
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String AUTHORIZATION_REQUEST_COOKIE_NAME = "authorization_request";

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String text = CookieHelper.get(request, AUTHORIZATION_REQUEST_COOKIE_NAME);
        return StringUtils.hasText(text) ? deserialize(text) : null;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieHelper.delete(request, response, AUTHORIZATION_REQUEST_COOKIE_NAME);
            return;
        }

        // If the cookie exists at the client side, it will be overwritten
        CookieHelper.Builder.newCookie(AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest))
                            .path("/")
                            .expiration(LoginAuthenticationEntryPoint.MAX_AGE)
                            .addTo(response);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            CookieHelper.delete(request, response, AUTHORIZATION_REQUEST_COOKIE_NAME);
            return authorizationRequest;
        } else {
            throw new OAuth2AuthenticationException(new OAuth2Error("authorization_not_found"),
                                                    "Can't find authorization request. You need to sign-in again.");
        }
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        return Base64.getUrlEncoder().encodeToString(
            SerializationUtils.serialize(authorizationRequest));
    }

    private OAuth2AuthorizationRequest deserialize(String cookie) {
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie));
    }
}
