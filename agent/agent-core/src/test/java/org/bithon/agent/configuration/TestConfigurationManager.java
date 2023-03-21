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

package org.bithon.agent.configuration;

import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/6 20:09
 */
public class TestConfigurationManager {

    @ConfigurationProperties(prefix = "test")
    static class TestConfig {
        private int a;
        private int b;

        public TestConfig() {
        }

        public TestConfig(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void test() throws IOException {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(Collections.singletonMap("test", new TestConfig(1, 7))));
        ConfigurationManager manager = ConfigurationManager.create(new Configuration(json));

        TestConfig testConfig = manager.getConfig(TestConfig.class);
        Assert.assertEquals(1, testConfig.getA());
        Assert.assertEquals(7, testConfig.getB());

        JsonNode json2 = objectMapper.readTree(objectMapper.writeValueAsBytes(Collections.singletonMap("test", new TestConfig(2, 8))));
        manager.refresh(new Configuration(json2));
        Assert.assertEquals(2, testConfig.getA());
        Assert.assertEquals(8, testConfig.getB());
    }
}
