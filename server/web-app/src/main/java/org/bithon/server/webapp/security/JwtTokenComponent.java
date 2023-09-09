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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;

/**
 * @author Frank Chen
 * @date 6/9/23 10:34 pm
 */
public class JwtTokenComponent {

    public static final String COOKIE_NAME_TOKEN = "token";
    private final SecretKey signKey;
    private final long validity;

    public JwtTokenComponent(SecurityConfig securityConfig) {
        this.signKey = Keys.hmacShaKeyFor(securityConfig.getJwtTokenSignKey().getBytes(StandardCharsets.UTF_8));
        this.validity = securityConfig.getJwtTokenValiditySeconds();
    }

    public Claims tokenToUser(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(signKey)
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

    public String userToToken(String name, Collection<? extends GrantedAuthority> authorities) {
        Claims claims = Jwts.claims().setSubject(name);
        claims.put("scopes", authorities);

        return Jwts.builder()
                   .setClaims(claims)
                   .setIssuer("https://bithon.org.cn")
                   .setIssuedAt(new Date(System.currentTimeMillis()))
                   .setExpiration(new Date(System.currentTimeMillis() + validity * 1000))
                   .signWith(signKey, SignatureAlgorithm.HS256)
                   .compact();
    }

    public boolean validateToken(Claims claims) {
        return claims.getSubject() != null && !isTokenExpired(claims);
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}