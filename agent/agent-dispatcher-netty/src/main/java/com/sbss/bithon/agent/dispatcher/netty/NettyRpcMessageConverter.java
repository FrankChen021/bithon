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

package com.sbss.bithon.agent.dispatcher.netty;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
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
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:13
 */
public class NettyRpcMessageConverter implements IMessageConverter {
    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, HttpClientCompositeMetric metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, JdbcPoolMetricSet metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, SqlCompositeMetric metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, MongoDbCompositeMetric counter) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, WebRequestCompositeMetric metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, JvmMetricSet metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, WebServerMetricSet metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, SqlStatementCompositeMetric counter) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, RedisClientCompositeMetric metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, ExceptionMetricSet metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, ThreadPoolCompositeMetric metric) {
        return null;
    }

    @Override
    public Object from(TraceSpan span) {
        return null;
    }

    @Override
    public Object from(EventMessage event) {
        return null;
    }

    @Override
    public Object from(Map<String, String> log) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, GcCompositeMetric gcMetricSet) {
        return null;
    }
}
