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
import org.bithon.server.commons.time.TimeSpan;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 25/9/23 2:59 pm
 */
public class JwtTokenComponentTest {
    private JwtTokenComponent component;

    @Before
    public void before() {
        component = new JwtTokenComponent(new JwtConfig());
    }

    @Test
    public void validateToken_UserValidity() {
        // Create a token with 2 day validity
        String jwt = component.createToken("frankchen@apache.org",
                                           Collections.emptyList(),
                                           "frankchen@apache.org",
                                           Duration.ofDays(2));
        Jws<Claims> token = component.parseToken(jwt);
        Assert.assertTrue(component.isValidToken(token));

        long expiration = component.getExpirationTimestamp(token);
        long diff = TimeSpan.of(expiration).diff(TimeSpan.now());
        long hours = diff / 1000 / 3600;
        Assert.assertTrue(hours == 47 || hours == 48);
    }

    @Test
    public void validateToken_UserValidity_Expired() throws InterruptedException {
        // Create a token with 1 second validity
        String jwt = component.createToken("frankchen@apache.org",
                                           Collections.emptyList(),
                                           "frankchen@apache.org",
                                           Duration.ofSeconds(1));
        // Wait for the token expired
        Thread.sleep(1200);

        Assert.assertThrows(ExpiredJwtException.class, () -> component.parseToken(jwt));
    }

    @Test
    public void validateToken_SystemValidity() {
        // Create a token with server side validity which is default to 1 day
        String jwt = component.createToken("frankchen@apache.org",
                                           Collections.emptyList(),
                                           "frankchen@apache.org",
                                           Duration.ZERO);
        Jws<Claims> token = component.parseToken(jwt);
        Assert.assertTrue(component.isValidToken(token));

        long expiration = component.getExpirationTimestamp(token);
        long diff = TimeSpan.of(expiration).diff(TimeSpan.now());
        long hours = diff / 1000 / 3600;
        Assert.assertTrue(hours == 23 || hours == 24);
    }

}
