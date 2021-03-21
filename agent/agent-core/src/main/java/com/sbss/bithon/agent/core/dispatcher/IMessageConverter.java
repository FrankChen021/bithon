package com.sbss.bithon.agent.core.dispatcher;

import com.sbss.bithon.agent.core.event.EventMessage;
import com.sbss.bithon.agent.core.metric.exception.ExceptionMetricSet;
import com.sbss.bithon.agent.core.metric.http.HttpClientMetricSet;
import com.sbss.bithon.agent.core.metric.jdbc.JdbcPoolMetricSet;
import com.sbss.bithon.agent.core.metric.jvm.JvmMetricSet;
import com.sbss.bithon.agent.core.metric.mongo.MongoDbMetricSet;
import com.sbss.bithon.agent.core.metric.redis.RedisClientMetric;
import com.sbss.bithon.agent.core.metric.sql.SqlMetricSet;
import com.sbss.bithon.agent.core.metric.sql.SqlStatementMetric;
import com.sbss.bithon.agent.core.metric.thread.ThreadPoolMetric;
import com.sbss.bithon.agent.core.metric.web.WebRequestMetricSet;
import com.sbss.bithon.agent.core.metric.web.WebServerMetricSet;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 10:46 下午
 */
public interface IMessageConverter {

    Object from(long timestamp, int interval, HttpClientMetricSet metric);

    Object from(long timestamp, int interval, JdbcPoolMetricSet metric);

    Object from(long timestamp, int interval, SqlMetricSet metric);

    Object from(long timestamp, int interval, MongoDbMetricSet counter);

    Object from(long timestamp, int interval, WebRequestMetricSet metric);

    Object from(long timestamp, int interval, JvmMetricSet metric);

    Object from(long timestamp, int interval, WebServerMetricSet metric);

    Object from(long timestamp, int interval, SqlStatementMetric counter);

    Object from(long timestamp, int interval, RedisClientMetric metric);

    Object from(long timestamp, int interval, ExceptionMetricSet metric);

    Object from(long timestamp, int interval, ThreadPoolMetric metric);

    // tracing span message
    Object from(TraceSpan span);

    Object from(EventMessage event);

    Object from(Map<String, String> log);
}
