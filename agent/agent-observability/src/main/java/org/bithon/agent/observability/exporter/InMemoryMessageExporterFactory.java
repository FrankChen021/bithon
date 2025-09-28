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

package org.bithon.agent.observability.exporter;


import org.bithon.agent.observability.event.EventMessage;
import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.agent.observability.metric.domain.jvm.JvmMetrics;
import org.bithon.agent.observability.metric.model.IMeasurement;
import org.bithon.agent.observability.metric.model.schema.Schema;
import org.bithon.agent.observability.metric.model.schema.Schema2;
import org.bithon.agent.observability.metric.model.schema.Schema3;
import org.bithon.agent.observability.tracing.context.ITraceSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ONLY for test cases
 *
 * @author frank.chen021@outlook.com
 * @date 28/9/25 6:03 pm
 */
public class InMemoryMessageExporterFactory implements IMessageExporterFactory {
    public static class InMemoryMetricExporter implements IMessageExporter {
        private static final List<Object> MESSAGES = Collections.synchronizedList(new ArrayList<>());

        public static void clear() {
            MESSAGES.clear();
        }

        public List<Object> getMessages() {
            return MESSAGES;
        }

        @Override
        public void export(Object message) {
            if (message instanceof Collection) {
                MESSAGES.addAll((Collection<?>) message);
            } else {
                MESSAGES.add(message);
            }
        }

        @Override
        public void close() throws Exception {
        }
    }

    public static class InMemoryTracingExporter implements IMessageExporter {
        private static final List<Object> MESSAGES = Collections.synchronizedList(new ArrayList<>());

        public static void clear() {
            MESSAGES.clear();
        }

        public List<Object> getMessages() {
            return MESSAGES;
        }

        @Override
        public void export(Object message) {
            if (message instanceof Collection) {
                MESSAGES.addAll((Collection<?>) message);
            } else {
                MESSAGES.add(message);
            }
        }

        @Override
        public void close() throws Exception {
        }
    }

    public static class RawMessageConverter implements IMessageConverter {
        @Override
        public Object from(long timestamp, int interval, JvmMetrics metrics) {
            return metrics;
        }

        @Override
        public Object from(ITraceSpan span) {
            return span;
        }

        @Override
        public Object from(EventMessage event) {
            return event;
        }

        @Override
        public Object from(Map<String, String> log) {
            return log;
        }

        @Override
        public Object from(Schema schema, Collection<IMeasurement> measurementList, long timestamp, int interval) {
            return measurementList;
        }

        @Override
        public Object from(Schema2 schema, Collection<IMeasurement> measurementList, long timestamp, int interval) {
            return measurementList;
        }

        @Override
        public Object from(Schema3 schema, List<Object[]> measurementList, long timestamp, int interval) {
            return measurementList;
        }
    }

    @Override
    public IMessageExporter createMetricExporter(ExporterConfig exporterConfig) {
        return new InMemoryMetricExporter();
    }

    @Override
    public IMessageExporter createTracingExporter(ExporterConfig exporterConfig) {
        return new InMemoryTracingExporter();
    }

    @Override
    public IMessageExporter createEventExporter(ExporterConfig exporterConfig) {
        return null;
    }

    @Override
    public IMessageConverter createMessageConverter() {
        return new RawMessageConverter();
    }
}
