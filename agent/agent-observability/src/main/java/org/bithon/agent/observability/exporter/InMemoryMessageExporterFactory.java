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


import org.bithon.agent.observability.exporter.config.ExporterConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        return null;
    }
}
