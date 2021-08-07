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

package com.sbss.bithon.agent.core.dispatcher;

import com.sbss.bithon.agent.core.event.EventMessage;
import com.sbss.bithon.agent.core.metric.domain.exception.ExceptionMetricSet;
import com.sbss.bithon.agent.core.metric.domain.http.HttpClientCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jdbc.JdbcPoolMetricSet;
import com.sbss.bithon.agent.core.metric.domain.jvm.GcCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jvm.JvmMetricSet;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.redis.RedisClientCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlStatementCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.thread.ThreadPoolCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.web.WebRequestCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.web.WebServerMetricSet;
import com.sbss.bithon.agent.core.tracing.context.ITraceSpan;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 10:46 下午
 */
public interface IMessageConverter {

    Object from(long timestamp, int interval, List<String> dimensions, HttpClientCompositeMetric metric);

    Object from(long timestamp, int interval, JdbcPoolMetricSet metric);

    Object from(long timestamp, int interval, List<String> dimensions, SqlCompositeMetric metric);

    Object from(long timestamp,
                int interval,
                List<String> dimensions,
                MongoDbCompositeMetric counter);

    Object from(long timestamp, int interval, List<String> dimensions, WebRequestCompositeMetric metric);

    Object from(long timestamp, int interval, JvmMetricSet metric);

    Object from(long timestamp, int interval, WebServerMetricSet metric);

    Object from(long timestamp, int interval, SqlStatementCompositeMetric counter);

    Object from(long timestamp, int interval, List<String> dimensions, RedisClientCompositeMetric metric);

    Object from(long timestamp, int interval, ExceptionMetricSet metric);

    Object from(long timestamp, int interval, ThreadPoolCompositeMetric metric);

    // tracing span message
    Object from(ITraceSpan span);

    Object from(EventMessage event);

    Object from(Map<String, String> log);

    Object from(long timestamp, int interval, GcCompositeMetric gcMetricSet);
}
