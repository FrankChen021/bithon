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

package org.bithon.agent.observability.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link HttpUtils}
 *
 * @author frank.chen021@outlook.com
 */
public class HttpUtilsTest {

    @Test
    public void testParseStatusLine_ValidStatusLines() {
        // Standard HTTP/1.1 responses
        Assertions.assertEquals("200", HttpUtils.parseStatusLine("HTTP/1.1 200 OK"));
        Assertions.assertEquals("404", HttpUtils.parseStatusLine("HTTP/1.1 404 Not Found"));
        Assertions.assertEquals("500", HttpUtils.parseStatusLine("HTTP/1.1 500 Internal Server Error"));
        Assertions.assertEquals("201", HttpUtils.parseStatusLine("HTTP/1.1 201 Created"));
        Assertions.assertEquals("401", HttpUtils.parseStatusLine("HTTP/1.1 401 Unauthorized"));
        Assertions.assertEquals("403", HttpUtils.parseStatusLine("HTTP/1.1 403 Forbidden"));
        Assertions.assertEquals("502", HttpUtils.parseStatusLine("HTTP/1.1 502 Bad Gateway"));
        Assertions.assertEquals("503", HttpUtils.parseStatusLine("HTTP/1.1 503 Service Unavailable"));

        // HTTP/1.0 responses
        Assertions.assertEquals("200", HttpUtils.parseStatusLine("HTTP/1.0 200 OK"));
        Assertions.assertEquals("301", HttpUtils.parseStatusLine("HTTP/1.0 301 Moved Permanently"));

        // HTTP/2 responses
        Assertions.assertEquals("200", HttpUtils.parseStatusLine("HTTP/2.0 200 OK"));

        // Status line with no reason phrase
        Assertions.assertEquals("204", HttpUtils.parseStatusLine("HTTP/1.1 204 "));
        
        // Status line with long reason phrase
        Assertions.assertEquals("400", HttpUtils.parseStatusLine("HTTP/1.1 400 Bad Request - Invalid Parameter"));
    }

    @Test
    public void testParseStatusLine_EdgeCases() {
        // All valid 3-digit status codes
        Assertions.assertEquals("100", HttpUtils.parseStatusLine("HTTP/1.1 100 Continue"));
        Assertions.assertEquals("999", HttpUtils.parseStatusLine("HTTP/1.1 999 Custom Status"));
        
        // Minimal valid status line
        Assertions.assertEquals("200", HttpUtils.parseStatusLine("H 200 "));
    }

    @Test
    public void testParseStatusLine_Interning() {
        // Test that interning works - same status code from different lines should return same instance
        String statusCode1 = HttpUtils.parseStatusLine("HTTP/1.1 200 OK");
        String statusCode2 = HttpUtils.parseStatusLine("HTTP/1.0 200 Success");
        String statusCode3 = HttpUtils.parseStatusLine("HTTP/2.0 200 ");
        
        // All should be interned to the same instance
        Assertions.assertSame(statusCode1, statusCode2);
        Assertions.assertSame(statusCode2, statusCode3);
        
        // Different status codes should not be the same instance
        String statusCode404 = HttpUtils.parseStatusLine("HTTP/1.1 404 Not Found");
        Assertions.assertNotSame(statusCode1, statusCode404);
        
        // But same 404 should be interned
        String statusCode404_2 = HttpUtils.parseStatusLine("HTTP/1.1 404 Error");
        Assertions.assertSame(statusCode404, statusCode404_2);
    }

    @Test
    public void testParseStatusLine_InvalidInputs() {
        // Null input
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine(null));

        // Empty string
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine(""));

        // No space
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1"));

        // Only one space
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 "));

        // Status code less than 3 digits
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 20"));
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 2"));

        // Status code more than 3 digits (no space after 3 digits)
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 2000"));
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 20000OK"));

        // Invalid characters in status code position (now validated, returns -1)
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 20X OK"));
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 ABC OK"));
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 2.0 OK"));

        // Missing space after status code
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 200OK"));
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1 200-OK"));

        // Too short string - need at least firstSpace + 5 chars total
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("A B"));
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("A B C")); // 5 chars, firstSpace=1, needs 6+ chars
    }

    @Test
    public void testParseStatusLine_WithWhitespace() {
        // Leading whitespace - first space is found in the leading spaces
        // So it tries to extract from that position which won't have valid format
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("  HTTP/1.1 200 OK"));
        
        // Multiple spaces between protocol and status code
        // First space is at position 8, so checks char at position 12 which should be space  
        // "HTTP/1.1  200 OK" - position 8 is first space, position 12 is '0', not a space
        Assertions.assertEquals("-1", HttpUtils.parseStatusLine("HTTP/1.1  200 OK"));
    }

    @Test
    public void testStatusCodeToString_CommonCodes() {
        // Test common status codes return cached strings
        Assertions.assertEquals("200", HttpUtils.statusCodeToString(200));
        Assertions.assertEquals("201", HttpUtils.statusCodeToString(201));
        Assertions.assertEquals("204", HttpUtils.statusCodeToString(204));
        Assertions.assertEquals("301", HttpUtils.statusCodeToString(301));
        Assertions.assertEquals("302", HttpUtils.statusCodeToString(302));
        Assertions.assertEquals("400", HttpUtils.statusCodeToString(400));
        Assertions.assertEquals("401", HttpUtils.statusCodeToString(401));
        Assertions.assertEquals("403", HttpUtils.statusCodeToString(403));
        Assertions.assertEquals("404", HttpUtils.statusCodeToString(404));
        Assertions.assertEquals("500", HttpUtils.statusCodeToString(500));
        Assertions.assertEquals("502", HttpUtils.statusCodeToString(502));
        Assertions.assertEquals("503", HttpUtils.statusCodeToString(503));

        // Verify same instance is returned (cached)
        Assertions.assertSame(HttpUtils.statusCodeToString(200), HttpUtils.statusCodeToString(200));
        Assertions.assertSame(HttpUtils.statusCodeToString(404), HttpUtils.statusCodeToString(404));
    }

    @Test
    public void testStatusCodeToString_UncommonCodes() {
        // Test uncommon but valid status codes (should be interned)
        Assertions.assertEquals("418", HttpUtils.statusCodeToString(418)); // I'm a teapot
        Assertions.assertEquals("451", HttpUtils.statusCodeToString(451)); // Unavailable For Legal Reasons
        Assertions.assertEquals("507", HttpUtils.statusCodeToString(507)); // Insufficient Storage

        // Verify interning works for valid codes
        String code1 = HttpUtils.statusCodeToString(418);
        String code2 = HttpUtils.statusCodeToString(418);
        Assertions.assertSame(code1, code2);
    }

    @Test
    public void testStatusCodeToString_InvalidCodes() {
        // Negative status codes (not interned)
        Assertions.assertEquals("-1", HttpUtils.statusCodeToString(-1));
        String neg1 = HttpUtils.statusCodeToString(-1);
        String neg2 = HttpUtils.statusCodeToString(-1);
        Assertions.assertNotSame(neg1, neg2); // Should not be interned

        // Zero (not interned)
        Assertions.assertEquals("0", HttpUtils.statusCodeToString(0));

        // Very large status codes (not interned, protection against DOS)
        Assertions.assertEquals("10000", HttpUtils.statusCodeToString(10000));
        String large1 = HttpUtils.statusCodeToString(99999);
        String large2 = HttpUtils.statusCodeToString(99999);
        Assertions.assertNotSame(large1, large2); // Should not be interned
    }

    @Test
    public void testDropQueryParameters() {
        // Standard cases
        Assertions.assertEquals("/api/users", HttpUtils.dropQueryParameters("/api/users?id=123"));
        Assertions.assertEquals("/api/users", HttpUtils.dropQueryParameters("/api/users?id=123&name=john"));
        Assertions.assertEquals("http://example.com/path", 
                          HttpUtils.dropQueryParameters("http://example.com/path?param=value"));

        // No query parameters
        Assertions.assertEquals("/api/users", HttpUtils.dropQueryParameters("/api/users"));
        Assertions.assertEquals("http://example.com", HttpUtils.dropQueryParameters("http://example.com"));

        // Empty query
        Assertions.assertEquals("/api/users", HttpUtils.dropQueryParameters("/api/users?"));

        // Multiple question marks
        Assertions.assertEquals("/api/users", HttpUtils.dropQueryParameters("/api/users?param=value?extra"));

        // Empty string
        Assertions.assertEquals("", HttpUtils.dropQueryParameters(""));

        // Just query parameters
        Assertions.assertEquals("", HttpUtils.dropQueryParameters("?id=123"));
    }
}

