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

package org.bithon.server.webapp;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import org.bithon.server.webapp.security.JwtTokenComponent;
import org.bithon.server.webapp.security.WebSecurityConfig;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 25/9/23 2:59 pm
 */
public class SecurityTest {

    @Test
    public void validateToken_UserValidity() {
        JwtTokenComponent component = new JwtTokenComponent(new WebSecurityConfig());
        String jwt = component.createToken("frankchen@apache.org", Collections.emptyList(),
                                           "frankchen@apache.org",
                                           Duration.ofDays(1).toMillis());
        Jws<Claims> token = component.parseToken(jwt);
        Assert.assertTrue(component.isValidToken(token));
    }

    @Test
    public void validateToken_UserValidity_Expired() throws InterruptedException {
        JwtTokenComponent component = new JwtTokenComponent(new WebSecurityConfig());
        String jwt = component.createToken("frankchen@apache.org", Collections.emptyList(),
                                           "frankchen@apache.org",
                                           Duration.ofSeconds(1).toMillis());
        // Wait for the token expired
        Thread.sleep(1200);

        Assert.assertThrows(ExpiredJwtException.class, () -> component.parseToken(jwt));
    }

    @Test
    public void validateToken_SystemValidity() {
        JwtTokenComponent component = new JwtTokenComponent(new WebSecurityConfig());
        String jwt = component.createToken("frankchen@apache.org", Collections.emptyList(),
                                           "frankchen@apache.org",
                                           0);
        Jws<Claims> token = component.parseToken(jwt);
        Assert.assertTrue(component.isValidToken(token));
    }

}
