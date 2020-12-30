package com.sbss.bithon.agent.plugin.alibaba.druid.metric;

import com.alibaba.druid.pool.DruidDataSource;
import com.sbss.bithon.agent.core.metrics.jdbc.JdbcMetric;
import com.sbss.bithon.agent.core.metrics.sql.SqlMetric;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/27 11:32 上午
 */
public class MonitoredSource {
    private final DruidDataSource dataSource;
    private final String driverType;
    private final String uri;
    private final JdbcMetric jdbcMetric;
    private final SqlMetric sqlMetric;

    MonitoredSource(String driverType,
                    String uri,
                    DruidDataSource dataSource) {
        this.dataSource = dataSource;
        this.driverType = driverType;
        this.uri = uri;
        this.jdbcMetric = new JdbcMetric(uri, driverType);
        this.sqlMetric = new SqlMetric(uri, driverType);
    }

    public DruidDataSource getDataSource() {
        return dataSource;
    }

    public String getDriverType() {
        return driverType;
    }

    public String getUri() {
        return uri;
    }

    public JdbcMetric getJdbcMetric() {
        return jdbcMetric;
    }

    public SqlMetric getSqlMetric() {
        return sqlMetric;
    }
}
