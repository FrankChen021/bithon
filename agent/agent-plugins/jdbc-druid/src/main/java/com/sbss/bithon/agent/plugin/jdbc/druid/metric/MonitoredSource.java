package com.sbss.bithon.agent.plugin.jdbc.druid.metric;

import com.alibaba.druid.pool.DruidDataSource;
import com.sbss.bithon.agent.core.metric.domain.jdbc.JdbcPoolMetricSet;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;

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
    private final SqlCompositeMetric sqlCompositeMetric;

    MonitoredSource(String driverClass,
                    String connectionString,
                    DruidDataSource dataSource) {
        this.dataSource = dataSource;
        this.driverClass = driverClass;
        this.connectionString = connectionString;
        this.jdbcPoolMetricSet = new JdbcPoolMetricSet(connectionString, driverClass);
        this.sqlCompositeMetric = new SqlCompositeMetric();
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

    public SqlCompositeMetric getSqlMetric() {
        return sqlCompositeMetric;
    }
}
