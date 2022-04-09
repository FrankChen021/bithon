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

package org.bithon.server.collector.source.http;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.server.sink.metrics.IMessageSink;
import org.bithon.server.sink.metrics.MetricMessage;
import org.bithon.server.sink.metrics.SchemaMetricMessage;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 2021-10-01
 */
@Slf4j
@RestController
@ConditionalOnProperty(value = "collector-http.enabled", havingValue = "true")
public class MetricHttpCollector {

    private final IMessageSink<SchemaMetricMessage> sink;

    public MetricHttpCollector(IMessageSink<SchemaMetricMessage> sink) {
        this.sink = sink;
    }

    @PostMapping(path = "/api/collector/metrics")
    public void saveMetrics(@RequestBody MetricOverHttp metrics) {
        log.trace("receive metrics:{}", metrics);

        final Iterator<Measurement> delegate = metrics.metrics.iterator();
        IteratorableCollection<MetricMessage> i = IteratorableCollection.of(new Iterator<MetricMessage>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public MetricMessage next() {
                Measurement m = delegate.next();

                MetricMessage message = new MetricMessage();
                message.putAll(m.getDimensions());
                message.putAll(m.getMetrics());
                return message;
            }
        });

        sink.process(metrics.getSchema().getName(),
                     SchemaMetricMessage.builder()
                                        .schema(metrics.getSchema())
                                        .metrics(i)
                                        .build());
    }

    @Data
    static class Measurement {
        private long timestamp;
        private long interval;
        private Map<String, String> dimensions;
        private Map<String, Number> metrics;
    }

    @Data
    static class MetricOverHttp {
        private DataSourceSchema schema;
        private List<Measurement> metrics;
    }
}
