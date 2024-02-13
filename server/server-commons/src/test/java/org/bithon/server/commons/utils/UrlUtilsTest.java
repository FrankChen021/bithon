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

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 18/9/23 8:29 pm
 */
public class UrlUtilsTest {

    @Test
    public void testParseParameters_Empty() {
        Assert.assertTrue(UrlUtils.parseURLParameters("http://localhost").isEmpty());
        Assert.assertTrue(UrlUtils.parseURLParameters("http://localhost?").isEmpty());
        Assert.assertTrue(UrlUtils.parseURLParameters("http://localhost?=").isEmpty());
    }

    @Test
    public void test2() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=");
        Assert.assertEquals(1, parameters.size());
        Assert.assertTrue(parameters.containsKey("m"));
        Assert.assertEquals("", parameters.get("m"));
    }

    @Test
    public void test3() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=&n=");
        Assert.assertEquals(2, parameters.size());
        Assert.assertEquals("", parameters.get("m"));
        Assert.assertEquals("", parameters.get("n"));
    }

    @Test
    public void test4() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=&n=&");
        Assert.assertEquals(2, parameters.size());
        Assert.assertEquals("", parameters.get("m"));
        Assert.assertEquals("", parameters.get("n"));
    }

    @Test
    public void test5() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=1&n==");
        Assert.assertEquals(2, parameters.size());
        Assert.assertEquals("1", parameters.get("m"));
        Assert.assertEquals("=", parameters.get("n"));
    }

    @Test
    public void test6() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m");
        Assert.assertEquals(0, parameters.size());
    }

    @Test
    public void test_Normal() {
        Map<String, String> parameters = UrlUtils.parseURLParameters("http://localhost?m=1&n=2&l=3&");
        Assert.assertEquals(3, parameters.size());
        Assert.assertEquals("1", parameters.get("m"));
        Assert.assertEquals("2", parameters.get("n"));
        Assert.assertEquals("3", parameters.get("l"));
    }

    @Test
    public void test_Sanitize() {
        // No value after the '='
        Assert.assertEquals("http://localhost?password=", UrlUtils.sanitize("http://localhost?password=", "password", "HIDDEN"));

        // Value is empty
        Assert.assertEquals("http://localhost?password=HIDDEN", UrlUtils.sanitize("http://localhost?password=    ", "password", "HIDDEN"));

        // Only one parameter
        Assert.assertEquals("http://localhost?password=HIDDEN", UrlUtils.sanitize("http://localhost?password=2", "password", "HIDDEN"));

        // More parameters, and it's at the end
        Assert.assertEquals("http://localhost?user=1&password=HIDDEN", UrlUtils.sanitize("http://localhost?user=1&password=2", "password", "HIDDEN"));

        // More parameters, and it's in the middle
        Assert.assertEquals("http://localhost?user=1&password=HIDDEN&database=default", UrlUtils.sanitize("http://localhost?user=1&password=2&database=default", "password", "HIDDEN"));

        // No parameters
        Assert.assertEquals("http://localhost", UrlUtils.sanitize("http://localhost", "localhost", "HIDDEN"));

        // No matched parameter
        Assert.assertEquals("http://localhost?p=1&q=2&r=3", UrlUtils.sanitize("http://localhost?p=1&q=2&r=3", "localhost", "HIDDEN"));

        // No host
        Assert.assertEquals("?query=select+1&password=HIDDEN", UrlUtils.sanitize("?query=select+1&password=2", "password", "HIDDEN"));

        // No host
        Assert.assertEquals("?password1=2&password=HIDDEN", UrlUtils.sanitize("?password1=2&password=3", "password", "HIDDEN"));

        Assert.assertEquals("?p1=1&pass=&p3=3", UrlUtils.sanitize("?p1=1&pass=&p3=3", "pass", "HIDDEN"));

    }
}
