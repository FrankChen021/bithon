package com.sbss.bithon.agent.dispatcher.metrics;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:39 下午
 */
public class MetricBase implements Serializable {
    public String applicationName;
    public String environment;
    public String ip;
    public int port;
    public int interval;
    public long timestamp;
}
