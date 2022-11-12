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

package org.bithon.server.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bithon.server.sink.tracing.LocalTraceSink;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaTraceConsumer extends AbstractKafkaConsumer<List<TraceSpan>> {
    private final LocalTraceSink traceSink;

    public KafkaTraceConsumer(LocalTraceSink traceSink) {
        super(new TypeReference<List<TraceSpan>>() {
        });

        this.traceSink = traceSink;
    }

    @Override
    protected void onMessage(String type, List<TraceSpan> spans) {
        traceSink.process(getTopic(), spans);
    }

    @Override
    public void stop() {
        // stop receiving
        try {
            super.stop();
        } catch (Exception ignored) {
        }

        try {
            traceSink.close();
        } catch (Exception ignored) {
        }
    }
}
