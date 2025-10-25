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

package org.bithon.server.pipeline.common.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 14/2/22 8:53 PM
 */
public class UriNormalizerTest {

    private final UriNormalizer normalizer = new UriNormalizer();

    @Test
    public void testNoChange() {
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "/");
        Assertions.assertEquals("/", r.getUri());
        Assertions.assertFalse(r.isNormalized());
    }

    @Test
    public void testUriWithParameters() {
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "/?query=select+1");
        Assertions.assertEquals("/", r.getUri());
        Assertions.assertTrue(r.isNormalized());
    }

    @Test
    public void testFullUri() {
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "http://localhost/?query=select+1");
        Assertions.assertEquals("http://localhost", r.getUri());
        Assertions.assertTrue(r.isNormalized());
    }

    @Test
    public void testMalformedUrlEncoding_PercentFollowedByNonHex() {
        // Test case from the issue: path contains "50%!1" where %! is not a valid URL-encoded sequence
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", 
            "/v4/spreadsheets/*/values/Mass judging casos com oferta < 50%!1:3445:clear");
        Assertions.assertNotNull(r.getUri());
        // Should not throw IllegalArgumentException
    }

    @Test
    public void testMalformedUrlEncoding_PercentAtEnd() {
        // Test case where % is at the end of a segment
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "/path/test%");
        Assertions.assertNotNull(r.getUri());
        // Should not throw IllegalArgumentException
    }

    @Test
    public void testMalformedUrlEncoding_PercentFollowedBySpace() {
        // Test case from the issue: "Error at index 0 in: ' P'"
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "/path/test% P");
        Assertions.assertNotNull(r.getUri());
        // Should not throw IllegalArgumentException
    }

    @Test
    public void testMalformedUrlEncoding_PercentFollowedBySingleChar() {
        // Test case where % is followed by only one character
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "/path/test%2");
        Assertions.assertNotNull(r.getUri());
        // Should not throw IllegalArgumentException
    }

    @Test
    public void testValidUrlEncoding() {
        // Test that valid URL encoding still works correctly
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "/path/test%20space");
        Assertions.assertNotNull(r.getUri());
        Assertions.assertTrue(r.getUri().contains("test space") || r.getUri().contains("*"));
    }
}
