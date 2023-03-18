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

package org.bithon.agent.observability.dispatcher;

import org.bithon.agent.observability.event.EventMessage;
import org.bithon.agent.observability.metric.collector.IMeasurement;
import org.bithon.agent.observability.metric.domain.jvm.JvmMetrics;
import org.bithon.agent.observability.metric.domain.sql.SQLMetrics;
import org.bithon.agent.observability.metric.domain.sql.SQLStatementMetrics;
import org.bithon.agent.observability.metric.model.schema.Schema;
import org.bithon.agent.observability.metric.model.schema.Schema2;
import org.bithon.agent.observability.metric.model.schema.Schema3;
import org.bithon.agent.observability.tracing.context.ITraceSpan;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 10:46 下午
 */
public interface IMessageConverter {

    Object from(long timestamp, int interval, List<String> dimensions, SQLMetrics metrics);

    Object from(long timestamp, int interval, JvmMetrics metrics);

    Object from(long timestamp, int interval, SQLStatementMetrics metrics);

    // tracing span message
    Object from(ITraceSpan span);

    Object from(EventMessage event);

    Object from(Map<String, String> log);

    Object from(Schema schema, Collection<IMeasurement> measurementList, long timestamp, int interval);

    Object from(Schema2 schema, Collection<IMeasurement> measurementList, long timestamp, int interval);

    Object from(Schema3 schema, List<Object[]> measurementList, long timestamp, int interval);
}
