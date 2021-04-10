/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.kafka.client;

import com.sbss.bithon.server.intf.metrics.IMetricsCollector;
import com.sbss.bithon.server.intf.metrics.jvm.JavaInstanceMetric;
import shaded.org.apache.kafka.clients.producer.KafkaProducer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 10:26 下午
 */
public class KafkaClient implements IMetricsCollector {

    final KafkaProducer producer;

    public KafkaClient() {
        Map<String, Object> properties = new HashMap<>();
        this.producer = new KafkaProducer(properties);
    }

    @Override
    public void sendJavaInstanceMetric(JavaInstanceMetric metrics) {
    }

    @Override
    public void sendWebServerMetrics() {

    }
}
