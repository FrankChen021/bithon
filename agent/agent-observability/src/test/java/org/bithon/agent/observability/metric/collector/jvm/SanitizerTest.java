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

package org.bithon.agent.observability.metric.collector.jvm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class SanitizerTest {
    @Test
    public void testSanitizeProperties_HidesSecretNames() {
        Map<String, String> input = new HashMap<>();
        input.put("password", "myPassword");
        input.put("secret", "mySecret");
        input.put("accessKey", "myAccessKey");
        input.put("token", "myToken");
        input.put("pwd", "myPwd");
        input.put("normalKey", "normalValue");

        Map<String, String> sanitized = JvmEventMessageBuilder.Sanitizer.sanitizeProperties(input);
    Assertions.assertEquals("HIDDEN", sanitized.get("password"));
    Assertions.assertEquals("HIDDEN", sanitized.get("secret"));
    Assertions.assertEquals("HIDDEN", sanitized.get("accessKey"));
    Assertions.assertEquals("HIDDEN", sanitized.get("token"));
    Assertions.assertEquals("HIDDEN", sanitized.get("pwd"));
    Assertions.assertEquals("normalValue", sanitized.get("normalKey"));
    }

    @Test
    public void testSanitizeProperties_HidesSecretValues() {
        Map<String, String> input = new HashMap<>();
        input.put("normalKey", "password=myPassword");
        input.put("anotherKey", "token='myToken'");
        input.put("yetAnotherKey", "secret=mySecret");
        input.put("key", "pwd=myPwd");
        input.put("key2", "accessKey=myAccessKey");
        input.put("key3", "noSecretHere");

        Map<String, String> sanitized = JvmEventMessageBuilder.Sanitizer.sanitizeProperties(input);
    Assertions.assertEquals("password=HIDDEN", sanitized.get("normalKey"));
    Assertions.assertEquals("token='HIDDEN'", sanitized.get("anotherKey"));
    Assertions.assertEquals("secret=HIDDEN", sanitized.get("yetAnotherKey"));
    Assertions.assertEquals("pwd=HIDDEN", sanitized.get("key"));
    Assertions.assertEquals("accessKey=HIDDEN", sanitized.get("key2"));
        Assert.assertEquals("noSecretHere", sanitized.get("key3"));
    }

    @Test
    public void testSanitizeProperties_MixedCases() {
        Map<String, String> input = new HashMap<>();
        input.put("Password", "myPassword");
        input.put("SECRET", "mySecret");
        input.put("AccessKey", "myAccessKey");
        input.put("TOKEN", "myToken");
        input.put("PWD", "myPwd");
        input.put("normalKey", "password=shouldHide");

        Map<String, String> sanitized = JvmEventMessageBuilder.Sanitizer.sanitizeProperties(input);
        Assert.assertEquals("HIDDEN", sanitized.get("Password"));
        Assert.assertEquals("HIDDEN", sanitized.get("SECRET"));
        Assert.assertEquals("HIDDEN", sanitized.get("AccessKey"));
        Assert.assertEquals("HIDDEN", sanitized.get("TOKEN"));
        Assert.assertEquals("HIDDEN", sanitized.get("PWD"));
        Assert.assertEquals("password=HIDDEN", sanitized.get("normalKey"));
    }
}
