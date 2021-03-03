package com.sbss.bithon.agent.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frankchen
 */
public class DispatcherConfig {

    private Map<String, Boolean> messageDebug = new HashMap<>();
    private DispatcherQueue queue;

    private String servers;

    private DispatcherClient client;

    public DispatcherQueue getQueue() {
        return queue;
    }

    public void setQueue(DispatcherQueue queue) {
        this.queue = queue;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public DispatcherClient getClient() {
        return client;
    }

    public void setClient(DispatcherClient client) {
        this.client = client;
    }

    public Map<String, Boolean> getMessageDebug() {
        return messageDebug;
    }

    public void setMessageDebug(Map<String, Boolean> messageDebug) {
        this.messageDebug = messageDebug;
    }
}
