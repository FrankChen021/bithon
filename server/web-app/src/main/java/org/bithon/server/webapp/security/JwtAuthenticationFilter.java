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
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.bithon.component.commons.utils.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;
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
    private final AntPathRequestMatcher apiRequestMatcher;

    public JwtAuthenticationFilter(JwtTokenComponent jwtTokenComponent) {
        this.jwtTokenComponent = jwtTokenComponent;
        this.apiRequestMatcher = new AntPathRequestMatcher("**/api/**");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String textToken = CookieHelper.get(req, JwtTokenComponent.COOKIE_NAME_TOKEN);
        if (textToken == null) {
            // Get the token from header to support APIs
            textToken = req.getHeader("X-Bithon-Token");
        }

        boolean authenticated = false;
        if (StringUtils.hasText(textToken)) {
            Jws<Claims> token = null;
            try {
                token = jwtTokenComponent.decodeToken(textToken);
            } catch (JwtException ignored) {
            }

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtTokenComponent.isValidToken(token)) {
                    //noinspection unchecked
                    Authentication authentication = new UsernamePasswordAuthenticationToken(token.getBody().getSubject(),
                                                                                            textToken,
                                                                                            (Collection<? extends GrantedAuthority>) token.getBody().get("scope"));

                    if (logger.isDebugEnabled()) {
                        logger.debug("authenticated user " + token + " ---> " + req.getRequestURI());
                    }

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    authenticated = true;
                }
            }
        }

        if (!authenticated && apiRequestMatcher.matches(req)) {
            throw new HTTPException(HttpStatus.UNAUTHORIZED.value());
        }

        chain.doFilter(req, res);
    }
}
