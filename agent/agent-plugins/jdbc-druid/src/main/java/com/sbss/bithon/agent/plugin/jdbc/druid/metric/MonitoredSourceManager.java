package com.sbss.bithon.agent.plugin.jdbc.druid.metric;

import com.alibaba.druid.pool.DruidDataSource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class MonitoredSourceManager {

    static final MonitoredSourceManager INSTANCE = new MonitoredSourceManager();
    private final Map<String, MonitoredSource> connectionMap = new ConcurrentHashMap<>();
    private final Map<DruidDataSource, MonitoredSource> dataSourceMap = new ConcurrentHashMap<>();

    static public MonitoredSourceManager getInstance() {
        return INSTANCE;
    }

    static public String parseConnectionString(String rawUrl) {
        // remove leading "jdbc:" prefix
        String str = rawUrl.replaceFirst("jdbc:", "");
        // remove parameters
        return str.split("\\?")[0];
    }

    public boolean addDataSource(DruidDataSource dataSource) {
        String uri = MonitoredSourceManager.parseConnectionString(dataSource.getRawJdbcUrl());
        if (connectionMap.containsKey(uri) && dataSourceMap.containsKey(dataSource)) {
            return false;
        }

        MonitoredSource monitoredSource = new MonitoredSource(dataSource.getDriverClassName(),
                                                              uri,
                                                              dataSource);
        connectionMap.putIfAbsent(uri, monitoredSource);
        dataSourceMap.putIfAbsent(dataSource, monitoredSource);
        return true;
    }

    public void rmvDataSource(DruidDataSource dataSource) {
        MonitoredSource monitoredSource = dataSourceMap.remove(dataSource);
        if (monitoredSource != null) {
            connectionMap.remove(monitoredSource.getConnectionString());
        }
    }

    public MonitoredSource getMonitoredDataSource(DruidDataSource dataSource) {
        return dataSourceMap.get(dataSource);
    }

    public MonitoredSource getMonitoredDataSource(String key) {
        return connectionMap.get(key);
    }

    public Collection<MonitoredSource> getMonitoredSources() {
        return connectionMap.values();
    }

    public boolean isEmpty() {
        return connectionMap.isEmpty();
    }
}
