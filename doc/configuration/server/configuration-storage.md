
This doc describes how to configure bithon to use different types of storage.
Currently, the following DBMS are supported:
1. H2
2. MySQL 
3. ClickHouse

> Note: 
> for H2 and MySQL, they're only for local development of this project.
> They're NOTE for the production development.

## Configuration example

```yaml
bithon:
  storage:
    tracing:
      provider: jdbc
      enabled: true
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    metric:
      provider: jdbc
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    event:
      provider: jdbc
      enabled: true
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    meta:
      provider: jdbc
      enabled: true
    setting:
      provider: jdbc
      enabled: true
    providers:
      jdbc:
        enabled: true
        type: h2
        # data source parameters
        url: jdbc:your_jdbc_connection_string
        username: your_user_name
        password: your_password
```

The `provider` property above references the property name in the `providers` property.
The

> NOTE:
> the user must have the privilege to create tables in a target database

## ClickHouse

### Prerequisite

The ClickHouse storage has been verified on 21.8.4 branch.

### Configurations

```yaml
bithon:
  storage:
    tracing:
      provider: clickhouse
      enabled: true
      ttl: 
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    metric:
      provider: clickhouse
      enabled: true
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    event:
      provider: clickhouse
      enabled: true
      ttl:
        enabled: true
        ttl: P7D
        cleanPeriod: PT30M
    meta:
      provider: clickhouse
    setting:
      provider: clickhouse
    providers:
      clickhouse:
        # data source parameters
        url: jdbc:clickhouse://your_clickhouse_addr:8123/your_clickhouse_databases
        username: your_clickhouse_name
        password: your_clickhouse_password
```

> NOTE: 
> - the database that you configured to the `url` parameter must be created in advance.
> - the user must have the privilege to create tables under the specified database

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

