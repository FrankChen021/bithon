package com.sbss.bithon.agent.core.dispatcher;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.events.EventMessage;
import com.sbss.bithon.agent.core.metrics.exception.ExceptionMetric;
import com.sbss.bithon.agent.core.metrics.http.HttpClientMetric;
import com.sbss.bithon.agent.core.metrics.jdbc.JdbcMetric;
import com.sbss.bithon.agent.core.metrics.jvm.JvmMetrics;
import com.sbss.bithon.agent.core.metrics.mongo.MongoMetric;
import com.sbss.bithon.agent.core.metrics.redis.RedisMetric;
import com.sbss.bithon.agent.core.metrics.sql.SqlMetric;
import com.sbss.bithon.agent.core.metrics.sql.SqlStatementMetric;
import com.sbss.bithon.agent.core.metrics.thread.AbstractThreadPoolMetrics;
import com.sbss.bithon.agent.core.metrics.web.WebRequestMetric;
import com.sbss.bithon.agent.core.metrics.web.WebServerMetric;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 10:46 下午
 */
public interface IMessageConverter {

    Object from(AppInstance appInstance, long timestamp, int interval, HttpClientMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, JdbcMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, SqlMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, MongoMetric counter);

    Object from(AppInstance appInstance, long timestamp, int interval, WebRequestMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, JvmMetrics metric);

    Object from(AppInstance appInstance, long timestamp, int interval, WebServerMetric metric);

    Object from(SqlStatementMetric counter);

    Object from(AppInstance appInstance, Map<String, String> map);

    Object from(AppInstance appInstance, long timestamp, int interval, RedisMetric metric);

    List<Object> from(AppInstance appInstance, long timestamp, int interval, Collection<ExceptionMetric> counters);

    Object from(AppInstance appInstance, long timestamp, int interval, List<AbstractThreadPoolMetrics> metricsList);

    // tracing span message
    Object from(AppInstance appInstance, List<TraceSpan> traceSpans);

    Object from(AppInstance appInstance, EventMessage message);


}
