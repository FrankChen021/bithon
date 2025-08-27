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

package org.bithon.server.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Customizer for handling async security and SSE endpoints
 */
@Slf4j
@Configuration
public class AsyncHttpRequestSecurityCustomizer {

    /**
     * Configures security to allow SSE endpoints to work without authentication checks
     * on every async event.
     */
    @Bean
    public IHttpSecurityCustomizer asyncSecurityCustomizer(ServerProperties serverProperties) {
        return httpSecurity -> {
            log.info("Configuring async security for SSE endpoints");

            String contextPath = StringUtils.hasText(serverProperties.getServlet().getContextPath())
                                 ? serverProperties.getServlet().getContextPath() : "";

            // Define SSE endpoints that should be excluded from re-authentication
            String[] sseEndpoints = Stream.of(
                "/api/diagnosis/continuous-thread-dump",
                "/api/diagnosis/continuous-profiling/start",
                "/api/agent/service/proxy/streaming"
            ).map(path -> contextPath + path).toArray(String[]::new);

            // Allow these endpoints without requiring authentication
            httpSecurity.authorizeHttpRequests(authorize ->
                                                   authorize.requestMatchers(sseEndpoints).permitAll()
            );

            // Configure security context to be saved between requests
            httpSecurity.securityContext(context ->
                                             context.requireExplicitSave(false)
            );
        };
    }

    /**
     * Filter that stores the security context for SSE endpoints in request attributes
     * to make it available for async processing
     */
    private static class SseSecurityContextPropagationFilter extends OncePerRequestFilter {
        private final String[] sseEndpoints;

        public SseSecurityContextPropagationFilter(String[] sseEndpoints) {
            this.sseEndpoints = sseEndpoints;
        }

        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull FilterChain filterChain) throws ServletException, IOException {

            String requestURI = request.getRequestURI();
            boolean isSseEndpoint = false;

            for (String endpoint : sseEndpoints) {
                if (requestURI.equals(endpoint)) {
                    isSseEndpoint = true;
                    break;
                }
            }

            if (isSseEndpoint) {
                // For SSE endpoints, store the current security context in a request attribute
                SecurityContext securityContext = SecurityContextHolder.getContext();
                if (securityContext != null && securityContext.getAuthentication() != null) {
                    log.debug("Storing security context for SSE endpoint: {}", requestURI);
                    request.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
                }
            }

            filterChain.doFilter(request, response);
        }
    }
}
