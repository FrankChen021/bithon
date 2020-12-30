package com.sbss.bithon.agent.plugin.alibaba.druid.metric;

import com.alibaba.druid.pool.DruidDataSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class MonitoredSourceManager {

    private enum DriverType {
        MYSQL("com.mysql", "mysql"),
        KYLIN("org.apache.kylin", "kylin"),
        UNKNOWN("unknown", "unknown_jdbc");

        private final String classIdentifier;
        private final String driverType;

        DriverType(String classIdentifier, String driverType) {
            this.classIdentifier = classIdentifier;
            this.driverType = driverType;
        }
    }

    private final Map<String, MonitoredSource> uriMap = new ConcurrentHashMap<>();
    private final Map<DruidDataSource, MonitoredSource> dataSourceMap = new ConcurrentHashMap<>();

    static final MonitoredSourceManager INSTANCE = new MonitoredSourceManager();

    static public MonitoredSourceManager getInstance() {
        return INSTANCE;
    }

    public boolean addDataSource(DruidDataSource dataSource) throws URISyntaxException {
        String uri = MonitoredSourceManager.parseDataSourceUri(dataSource.getRawJdbcUrl());
        if (uriMap.containsKey(uri) && dataSourceMap.containsKey(dataSource)) {
            return false;
        }

        MonitoredSource monitoredSource = new MonitoredSource(getDriverType(dataSource.getDriverClassName()),
                                                              uri,
                                                              dataSource);
        uriMap.putIfAbsent(uri, monitoredSource);
        dataSourceMap.putIfAbsent(dataSource, monitoredSource);
        return true;
    }

    public void rmvDataSource(DruidDataSource dataSource) {
        MonitoredSource monitoredSource = dataSourceMap.remove(dataSource);
        if (monitoredSource != null) {
            uriMap.remove(monitoredSource.getUri());
        }
    }

    public MonitoredSource getDataSource(DruidDataSource dataSource) {
        return dataSourceMap.get(dataSource);
    }

    public MonitoredSource getDataSource(String key) {
        return uriMap.get(key);
    }

    public Collection<MonitoredSource> getDataSources() {
        return uriMap.values();
    }

    String getDriverType(String driverClassName) {
        for (DriverType driverType : DriverType.values()) {
            if (driverClassName.toLowerCase().contains(driverType.classIdentifier)) {
                return driverType.driverType;
            }
        }

        return DriverType.UNKNOWN.driverType;
    }

    public boolean isEmpty() {
        return uriMap.isEmpty();
    }

    static public String parseDataSourceUri(String rawUrl) throws URISyntaxException {
        // remove leading "jdbc:" prefix
        String originUrl = rawUrl.replaceFirst("jdbc:", "");

        URI uri = new URI(originUrl);
        return uri.getHost() + ":" + uri.getPort();
    }
}
