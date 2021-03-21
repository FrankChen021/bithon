package com.sbss.bithon.agent.plugin.jdbc.druid.metric;

import com.alibaba.druid.pool.DruidDataSource;
import com.sbss.bithon.agent.core.metric.jdbc.JdbcPoolMetricSet;
import com.sbss.bithon.agent.core.metric.sql.SqlMetricSet;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/27 11:32 上午
 */
public class MonitoredSource {
    private final DruidDataSource dataSource;
    private final String driverClass;

    // dimension
    private final String connectionString;

    // metrics
    private final JdbcPoolMetricSet jdbcPoolMetricSet;
    private final SqlMetricSet sqlMetricSet;

    MonitoredSource(String driverClass,
                    String connectionString,
                    DruidDataSource dataSource) {
        this.dataSource = dataSource;
        this.driverClass = driverClass;
        this.connectionString = connectionString;
        this.jdbcPoolMetricSet = new JdbcPoolMetricSet(connectionString, driverClass);
        this.sqlMetricSet = new SqlMetricSet(connectionString, driverClass);
    }

    public DruidDataSource getDataSource() {
        return dataSource;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public JdbcPoolMetricSet getJdbcMetric() {
        return jdbcPoolMetricSet;
    }

    public SqlMetricSet getSqlMetric() {
        return sqlMetricSet;
    }
}
