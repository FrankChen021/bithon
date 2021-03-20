package com.sbss.bithon.agent.core.dispatcher;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.event.EventMessage;
import com.sbss.bithon.agent.core.metric.exception.ExceptionMetric;
import com.sbss.bithon.agent.core.metric.http.HttpClientMetric;
import com.sbss.bithon.agent.core.metric.jdbc.JdbcPoolMetric;
import com.sbss.bithon.agent.core.metric.jvm.JvmMetrics;
import com.sbss.bithon.agent.core.metric.mongo.MongoMetric;
import com.sbss.bithon.agent.core.metric.redis.RedisMetric;
import com.sbss.bithon.agent.core.metric.sql.SqlMetric;
import com.sbss.bithon.agent.core.metric.sql.SqlStatementMetric;
import com.sbss.bithon.agent.core.metric.thread.ThreadPoolMetric;
import com.sbss.bithon.agent.core.metric.web.WebRequestMetric;
import com.sbss.bithon.agent.core.metric.web.WebServerMetric;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 10:46 下午
 */
public interface IMessageConverter {

    Object from(AppInstance appInstance, long timestamp, int interval, HttpClientMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, JdbcPoolMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, SqlMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, MongoMetric counter);

    Object from(AppInstance appInstance, long timestamp, int interval, WebRequestMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, JvmMetrics metric);

    Object from(AppInstance appInstance, long timestamp, int interval, WebServerMetric metric);

    Object from(SqlStatementMetric counter);

    Object from(AppInstance appInstance, Map<String, String> map);

    Object from(AppInstance appInstance, long timestamp, int interval, RedisMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, ExceptionMetric metric);

    Object from(AppInstance appInstance, long timestamp, int interval, ThreadPoolMetric metric);

    // tracing span message
    Object from(TraceSpan span);

    Object from(AppInstance appInstance, EventMessage event);


}
