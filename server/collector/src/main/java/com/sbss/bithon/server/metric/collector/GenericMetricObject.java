package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.server.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.server.meta.EndPointLink;
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

    public String getApplicationEnv() {
        return (String) get("env");
    }

    public String getInstanceName() {
        return (String) get("instanceName");
    }

    public EndPointLink getEndPointLink() {
        return (EndPointLink) get("endpointLink");
    }

    public void setEndpointLink(EndPointType srcEndPointType,
                                String srcEndpoint,
                                EndPointType dstEndPointType,
                                String dstEndpoint) {

        put("endpointLink", new EndPointLink(srcEndPointType,
                                             srcEndpoint,
                                             dstEndPointType,
                                             dstEndpoint));
    }

    public String getDimension(String name) {
        return (String) get(name);
    }
}
