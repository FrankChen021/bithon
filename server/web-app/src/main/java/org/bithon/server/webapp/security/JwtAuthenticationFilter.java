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
import io.jsonwebtoken.JwtException;
import org.bithon.component.commons.utils.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * A filter before {@link org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter}.
 * <p>
 * If successfully authenticated,
 * the {@link SecurityContextHolder} is updated to hold authentication for current request.
 *
 * @author Frank Chen
 * @date 6/9/23 10:36 pm
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenComponent jwtTokenComponent;

    public JwtAuthenticationFilter(JwtTokenComponent jwtTokenComponent) {
        this.jwtTokenComponent = jwtTokenComponent;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String token = CookieHelper.get(req, JwtTokenComponent.COOKIE_NAME_TOKEN);
        if (token == null) {
            // Get the token from header to support APIs
            token = req.getHeader("X-Bithon-Token");
        }

        if (StringUtils.hasText(token)) {
            Claims user = null;
            try {
                user = jwtTokenComponent.tokenToUser(token);
            } catch (JwtException ignored) {
            }

            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtTokenComponent.validateToken(user)) {
                    //noinspection unchecked
                    Authentication authentication = new UsernamePasswordAuthenticationToken(user.getSubject(),
                                                                                            null,
                                                                                            (Collection<? extends GrantedAuthority>) user.get("scope"));

                    if (logger.isDebugEnabled()) {
                        logger.debug("authenticated user " + user + " ---> " + req.getRequestURI());
                    }
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        chain.doFilter(req, res);
    }
}
