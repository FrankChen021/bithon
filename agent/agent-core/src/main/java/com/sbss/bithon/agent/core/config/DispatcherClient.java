package com.sbss.bithon.agent.core.config;

/**
 * @author frankchen
 */
public class DispatcherClient {

    private String factory;
    private int timeout;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }
}
