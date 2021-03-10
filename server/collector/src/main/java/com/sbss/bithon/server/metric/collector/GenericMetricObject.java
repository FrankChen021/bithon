package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.server.common.utils.ReflectionUtils;
import com.sbss.bithon.server.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.server.meta.EndPointLink;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 9:47 下午
 */
public class GenericMetricObject {

    private Map<String, Object> values = new HashMap<>();

    public GenericMetricObject(long timestamp,
                               String applicationName,
                               String instanceName,
                               Object properties) {
        ReflectionUtils.getFields(properties, values);
        values.put("timestamp", DateTimeUtils.dropMilliseconds(timestamp));
        values.put("appName", applicationName);
        values.put("instanceName", instanceName);
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

    public EndPointLink getEndPointLink() {
        return (EndPointLink) values.get("endpointLink");
    }

    public void setEndpointLink(EndPointType srcEndPointType,
                                String srcEndpoint,
                                EndPointType dstEndPointType,
                                String dstEndpoint) {

        values.put("endpointLink", new EndPointLink(srcEndPointType,
                                                    srcEndpoint,
                                                    dstEndPointType,
                                                    dstEndpoint));
    }

    public String getDimension(String name) {
        return (String) values.get(name);
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void put(String prop, Object value) {
        values.put(prop, value);
    }

    public void merge(Object object) {
        ReflectionUtils.getFields(object, this.values);
    }
}
