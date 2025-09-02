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

package org.bithon.agent.plugin.test.kafka;

import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.apache.kafka0102.Kafka0102Plugin;
import org.bithon.agent.plugin.test.AbstractPluginInterceptorTest;

/**
 * Test case for Apache Kafka 2.7.x plugin
 *
 * @author frankchen
 */
public class Kafka0102PluginInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin getPlugin() {
        return new Kafka0102Plugin();
    }
}
