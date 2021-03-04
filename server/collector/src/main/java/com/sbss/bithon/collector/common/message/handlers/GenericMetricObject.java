package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.collector.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.component.db.dao.EndPointType;

import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 9:47 下午
 */
public class GenericMetricObject extends HashMap<String, Object> {

    public GenericMetricObject(long timestamp,
                               String applicationName,
                               String instanceName) {
        put("timestamp", DateTimeUtils.dropMilliseconds(timestamp));
        put("appName", applicationName);
        put("instanceName", instanceName);
    }

    public long getTimestamp() {
        return (long) get("timestamp");
    }

    public String getApplicationName() {
        return (String) get("appName");
    }

    public String getInstanceName() {
        return (String) get("instanceName");
    }

    public String getTargetEndpoint() {
        return (String) get("targetEndpoint");
    }

    public EndPointType getTargetEndpointType() {
        return (EndPointType) get("targetEndpointType");
    }

    public void setTargetEndpoint(EndPointType endPointType,
                                  String endpoint) {
        put("targetEndpoint", endpoint);
        put("targetEndpointType", endPointType);
    }

    public String getString(String name) {
        return (String)get(name);
    }
}
