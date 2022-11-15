
This doc describes how to configure bithon to use different types of storage.
Currently, two types of storages are supported:
1. DBMS that supports standard JDBC such as H2, MySQL
2. ClickHouse

## JDBC
```yaml
bithon:
  storage:
    tracing:
      type: jdbc
      enabled: true
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    metric:
      type: jdbc
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    event:
      type: jdbc
      enabled: true
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    meta:
      enabled: true
      type: jdbc
    setting:
      enabled: true
      type: jdbc
    providers:
      jdbc:
        enabled: true
        # data source parameters
        url: jdbc:your_jdbc_connection_string
        username: your_user_name
        password: your_password
```

> NOTE:
> the user must have privilege to create tables in target database

## ClickHouse

### Prerequisite

The ClickHouse storage has been verified on 21.8.4 branch.

### Configurations

```yaml
bithon:
  storage:
    tracing:
      type: clickhouse
      enabled: true
      ttl: 
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    metric:
      type: clickhouse
      enabled: true
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    event:
      type: clickhouse
      enabled: true
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    meta:
      type: clickhouse
    setting:
      type: clickhouse
    providers:
      clickhouse:
        enabled: true
        # data source parameters
        url: jdbc:clickhouse://your_clickhouse_addr:8123/your_clickhouse_databases
        driverClassName: ru.yandex.clickhouse.ClickHouseDriver
        username: your_clickhouse_name
        password: your_clickhouse_password
```

> NOTE: 
> - the database that you configured to the `url` parameter must be created in advance.
> - the user must have privilege to create tables under the specified database

#### Table Configuration

By default, `MergeTree` engine is used. You can configure to use other `MergeTree` family such as `ReplicatedMergeTree`.


```yaml
bithon:
  storage:
    providers:
      clickhouse:
        cluster: your_cluster_name
        engine: your_merge_tree_engine
        createTableSettings: extra_table_settings
        onDistributedTable: false
```

| Path                | Description                                                                                 | Default   | Nullable |
|---------------------|---------------------------------------------------------------------------------------------|-----------|----------|
| cluster             | ClickHouse cluster name if you deploy ClickHouse in cluster mode.                           |           | true     |
| engine              | Table engine of tables that Bithon creates.                                                 | MergeTree | true     |
| createTableSettings | Extra table settings.                                                                       |           | true     |
| onDistributedTable  | If tables are distributed-based. If true, there will be a distributed table for each table. | false     | true     |

Example:
```yaml
bithon:
  storage:
    providers:
      clickhouse:
        engine: ReplicatedMergeTree('/clickhouse/tables/{shard}-{layer}/{database}.{table}', '{replica}')
        createTableSettings: storage_policy = 's3'
```

