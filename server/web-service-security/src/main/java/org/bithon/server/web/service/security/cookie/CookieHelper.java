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

package org.bithon.server.web.service.security.cookie;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Frank Chen
 * @date 8/9/23 12:06 am
 */
public class CookieHelper {

    public static class Builder {
        private Cookie cookie;

        public static Builder newCookie(String name, String content) {
            Builder builder = new Builder();
            builder.cookie = new Cookie(name, content);
            builder.cookie.setHttpOnly(true);
            return builder;
        }

        public Builder expiration(long expirationSeconds) {
            cookie.setMaxAge((int) expirationSeconds);
            return this;
        }

        public Builder path(String path) {
            cookie.setPath(path);
            return this;
        }

        public void addTo(HttpServletResponse response) {
            response.addCookie(cookie);
        }
    }

    public static String get(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public static void delete(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

}
