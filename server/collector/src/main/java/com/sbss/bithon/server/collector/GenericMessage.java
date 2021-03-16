package com.sbss.bithon.server.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.*;
import com.sbss.bithon.server.common.utils.ReflectionUtils;

import java.util.Map;

/**
 * TODO: cache reflection results
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class GenericMessage {

    protected Map<String, Object> values;

    public GenericMessage(Map<String, Object> values) {
        this.values = values;
    }

    public long getTimestamp() {
        return (long) values.get("timestamp");
    }

    public String getApplicationName() {
        return (String) values.get("appName");
    }

    public String getApplicationEnv() {
        return (String) values.get("env");
    }

    public String getInstanceName() {
        return (String) values.get("instanceName");
    }

    public long getLong(String prop) {
        return (long) values.getOrDefault(prop, 0L);
    }

    public Object get(String prop) {
        return values.get(prop);
    }

    public <T> T getAs(String prop) {
        return (T) values.get(prop);
    }

    public void set(String prop, Object value) {
        values.put(prop, value);
    }

    public String getString(String prop) {
        return (String) values.get(prop);
    }

    public static GenericMessage of(MessageHeader header, WebRequestMetricMessage message) {
        Map<String, Object> map = ReflectionUtils.getFields(header);
        return new GenericMessage(ReflectionUtils.getFields(message, map));
    }

    public static GenericMessage of(MessageHeader header, JvmMetricMessage message) {
        Map<String, Object> map = ReflectionUtils.getFields(header);
        ReflectionUtils.getFields(message.classesEntity, map);
        ReflectionUtils.getFields(message.cpuEntity, map);
        ReflectionUtils.getFields(message.heapEntity, map);
        ReflectionUtils.getFields(message.instanceTimeEntity, map);
        ReflectionUtils.getFields(message.memoryEntity, map);
        ReflectionUtils.getFields(message.nonHeapEntity, map);
        ReflectionUtils.getFields(message.metaspaceEntity, map);
        ReflectionUtils.getFields(message.gcEntities, map);
        ReflectionUtils.getFields(message.threadEntity, map);

        map.put("interval", message.interval);
        map.put("timestamp", message.timestamp);
        return new GenericMessage(map);
    }

    public static GenericMessage of(MessageHeader header, ExceptionMetricMessage message) {
        Map<String, Object> map = ReflectionUtils.getFields(header);
        return new GenericMessage(ReflectionUtils.getFields(message, map));
    }

    public static GenericMessage of(MessageHeader header, HttpClientMetricMessage message) {
        Map<String, Object> map = ReflectionUtils.getFields(header);
        return new GenericMessage(ReflectionUtils.getFields(message, map));
    }

    public static GenericMessage of(MessageHeader header, WebServerMetricMessage message) {
        Map<String, Object> map = ReflectionUtils.getFields(header);
        return new GenericMessage(ReflectionUtils.getFields(message, map));
    }

    public static GenericMessage of(MessageHeader header, JdbcPoolMetricMessage message) {
        Map<String, Object> map = ReflectionUtils.getFields(header);
        return new GenericMessage(ReflectionUtils.getFields(message, map));
    }

    public static GenericMessage of(MessageHeader header, RedisMetricMessage message) {
        Map<String, Object> map = ReflectionUtils.getFields(header);
        return new GenericMessage(ReflectionUtils.getFields(message, map));
    }

    public static GenericMessage of(MessageHeader header, ThreadPoolMetricMessage message) {
        Map<String, Object> map = ReflectionUtils.getFields(header);
        return new GenericMessage(ReflectionUtils.getFields(message, map));
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
