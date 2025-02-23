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

package org.bithon.server.web.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;

/**
 * @author Frank Chen
 * @date 6/9/23 10:34 pm
 */
public class JwtTokenComponent {

    public static final String COOKIE_NAME_TOKEN = "token";
    private final SecretKey signKey;

    @Getter
    private final long globalValidityMilliseconds;

    public JwtTokenComponent(JwtConfig jwtConfig) {
        this.signKey = Keys.hmacShaKeyFor(jwtConfig.getJwtTokenSignKey().getBytes(StandardCharsets.UTF_8));
        this.globalValidityMilliseconds = jwtConfig.getJwtTokenValiditySeconds() * 1000L;
    }

    /**
     * @throws io.jsonwebtoken.ExpiredJwtException
     */
    public Jws<Claims> parseToken(String tokenText) {
        return Jwts.parserBuilder()
                   .setSigningKey(signKey)
                   .build()
                   .parseClaimsJws(tokenText);
    }

    /**
     * Try to parse and return a valid token
     */
    public Jws<Claims> tryParseToken(String tokenText) {
        try {
            Jws<Claims> token = Jwts.parserBuilder()
                                    .setSigningKey(signKey)
                                    .build()
                                    .parseClaimsJws(tokenText);
            return isValidToken(token) ? token : null;
        } catch (ExpiredJwtException ignored) {
            return null;
        }
    }

    public String createToken(String name, Collection<? extends GrantedAuthority> authorities) {
        return createToken(name, authorities, "system", Duration.ZERO);
    }

    public String createToken(String name,
                              Collection<? extends GrantedAuthority> authorities,
                              String issuer,
                              Duration duration) {
        Claims claims = Jwts.claims().setSubject(name);
        claims.put("scopes", authorities);

        JwtBuilder builder = Jwts.builder()
                                 .setClaims(claims)
                                 .setIssuer("https://bithon.org/token/issuer/" + issuer)
                                 .setIssuedAt(new Date(System.currentTimeMillis()))
                                 .signWith(signKey, SignatureAlgorithm.HS256);
        if (!duration.isZero() && !duration.isNegative()) {
            // Set the expiration in the token.
            // If it's expired, the decodeToken function call above will fail to decode the token
            builder.setExpiration(new Date(duration.toMillis() + System.currentTimeMillis()));
        }

        return builder.compact();
    }

    public boolean isValidToken(Jws<Claims> token) {
        long expiredAt = getExpirationTimestamp(token);
        return System.currentTimeMillis() < expiredAt;
    }

    /**
     * Get the timestamp of expiration of a given token
     */
    public long getExpirationTimestamp(Jws<Claims> token) {
        Claims claims = token.getBody();
        if (claims == null || claims.getSubject() == null) {
            return 0;
        }

        if (claims.getExpiration() != null) {
            return claims.getExpiration().getTime();
        } else {
            // Use the server side validity
            return claims.getIssuedAt().getTime() + globalValidityMilliseconds;
        }
    }
}
