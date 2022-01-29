

- `agent.plugin.jdbc.druid.isSQLMetricEnabled`
  - Dynamic configuration is **NOT** supported now.
  - Boolean, default is `false`
  - Whether Enable the SQL metrics. If the application has no plugin support for the underlying database driver, 
    this configuration can be set to `true` to get metrics from the Druid connection pool layer. 
    
    However, if there's plugin support, for example MySQL is supported, this should be kept as `false`, or the metrics will be statistic twice.  