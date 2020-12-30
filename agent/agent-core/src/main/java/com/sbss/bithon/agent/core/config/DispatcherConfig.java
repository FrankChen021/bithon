package com.sbss.bithon.agent.core.config;

import java.util.List;

public class DispatcherConfig {

    private DispatcherQueue queue;

    private List<DispatcherServer> servers;

    private DispatcherClient client;

    public DispatcherQueue getQueue() {
        return queue;
    }

    public void setQueue(DispatcherQueue queue) {
        this.queue = queue;
    }

    public List<DispatcherServer> getServers() {
        return servers;
    }

    public void setServers(List<DispatcherServer> servers) {
        this.servers = servers;
    }

    public DispatcherClient getClient() {
        return client;
    }

    public void setClient(DispatcherClient client) {
        this.client = client;
    }
}
