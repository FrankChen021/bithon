package com.sbss.bithon.agent.core.metrics.web;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 10:53 下午
 */
public enum WebServerType {
    TOMCAT("tomcat"),
    UNDERTOW("undertow"),
    JETTY("jetty");

    private final String type;

    WebServerType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }
}
