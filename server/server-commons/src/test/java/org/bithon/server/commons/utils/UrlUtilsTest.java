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

package org.bithon.server.commons.utils;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import java.util.Map;

/**
 * @author Frank Chen
 * @date 18/9/23 8:29 pm
 */
public class UrlUtilsTest {

    @Test
    public void test_ParseParameters_EmptyQueryString() {
        Assertions.assertTrue(UrlUtils.parseURLParameters("http://localhost").isEmpty());
        Assertions.assertTrue(UrlUtils.parseURLParameters("http://localhost?").isEmpty());
        Assertions.assertTrue(UrlUtils.parseURLParameters("http://localhost?=").isEmpty());
    }

    @Test
    public void test_ParseParameters_EmptyValue() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=");
        Assertions.assertEquals(1, parameters.size());
        Assertions.assertTrue(parameters.containsKey("m"));
        Assertions.assertEquals("", parameters.get("m"));
    }

    @Test
    public void test_ParseParameters_MultipleEmptyValues() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=&n=");
        Assertions.assertEquals(2, parameters.size());
        Assertions.assertEquals("", parameters.get("m"));
        Assertions.assertEquals("", parameters.get("n"));
    }

    @Test
    public void test4() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=&n=&");
        Assertions.assertEquals(2, parameters.size());
        Assertions.assertEquals("", parameters.get("m"));
        Assertions.assertEquals("", parameters.get("n"));
    }

    @Test
    public void test5() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=1&n==");
        Assertions.assertEquals(2, parameters.size());
        Assertions.assertEquals("1", parameters.get("m"));
        Assertions.assertEquals("=", parameters.get("n"));
    }

    @Test
    public void test6() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m");
        Assertions.assertEquals(1, parameters.size());
    }

    @Test
    public void test_ParseParameters_Normal() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=1&n=2&l=3&");
        Assertions.assertEquals(3, parameters.size());
        Assertions.assertEquals("1", parameters.get("m"));
        Assertions.assertEquals("2", parameters.get("n"));
        Assertions.assertEquals("3", parameters.get("l"));
    }

    @Test
    public void test_ParseParameters_WhiteList() {
        {
            Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=1&n=2&l=3&", ImmutableSet.of("n"));
            Assertions.assertEquals(1, parameters.size());
            Assertions.assertEquals("2", parameters.get("n"));
        }
        {
            Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=1&n=2&l=3&", ImmutableSet.of("x"));
            Assertions.assertEquals(0, parameters.size());
        }
    }

    @Test
    public void test_Sanitize() {
        // No value after the '='
        Assertions.assertEquals("http://localhost?password=", UrlUtils.sanitize("http://localhost?password=", "password", "HIDDEN"));

        // Value is empty
        Assertions.assertEquals("http://localhost?password=HIDDEN", UrlUtils.sanitize("http://localhost?password=    ", "password", "HIDDEN"));

        // Only one parameter
        Assertions.assertEquals("http://localhost?password=HIDDEN", UrlUtils.sanitize("http://localhost?password=2", "password", "HIDDEN"));

        // More parameters, and it's at the end
        Assertions.assertEquals("http://localhost?user=1&password=HIDDEN", UrlUtils.sanitize("http://localhost?user=1&password=2", "password", "HIDDEN"));

        // More parameters, and it's in the middle
        Assertions.assertEquals("http://localhost?user=1&password=HIDDEN&database=default", UrlUtils.sanitize("http://localhost?user=1&password=2&database=default", "password", "HIDDEN"));

        // No parameters
        Assertions.assertEquals("http://localhost", UrlUtils.sanitize("http://localhost", "localhost", "HIDDEN"));

        // No matched parameter
        Assertions.assertEquals("http://localhost?p=1&q=2&r=3", UrlUtils.sanitize("http://localhost?p=1&q=2&r=3", "localhost", "HIDDEN"));

        // No host
        Assertions.assertEquals("?query=select+1&password=HIDDEN", UrlUtils.sanitize("?query=select+1&password=2", "password", "HIDDEN"));

        // No host
        Assertions.assertEquals("?password1=2&password=HIDDEN", UrlUtils.sanitize("?password1=2&password=3", "password", "HIDDEN"));

        Assertions.assertEquals("?p1=1&pass=&p3=3", UrlUtils.sanitize("?p1=1&pass=&p3=3", "pass", "HIDDEN"));

    }
}
