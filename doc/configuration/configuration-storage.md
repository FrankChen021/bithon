
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
      ttl: P1D
      cleanPeriod: PT3M
    metric:
      type: jdbc
      ttl: P1D
      cleanPeriod: PT3M
    event:
      type: jdbc
      ttl: P1D
      cleanPeriod: PT3M
    meta:
      type: jdbc
    setting:
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

Window functionality must be enabled on your ClickHouse cluster.

```xml
<!-- server/users.d/allow_experimental_window_functions.xml -->
<?xml version="1.0"?>
<yandex>
   <profiles>
       <default>
           <allow_experimental_window_functions>1</allow_experimental_window_functions>
       </default>
   </profiles>
</yandex>
```

### Configurations

```yaml
bithon:
  storage:
    tracing:
      type: clickhouse
      ttl: P7D
      cleanPeriod: PT30M
    metric:
      type: clickhouse
      ttl: P7D
      cleanPeriod: PT30M
    event:
      type: clickhouse
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

#### Configure to use a ClickHouse cluster

```yaml
bithon:
  storage:
    providers:
      clickhouse:
        cluster: your_cluster_name
```

#### MergeTree engine configuration

By default, `MergeTree` engine is used. You can configure to use other `MergeTree` family such as `ReplicatedMergeTree`.

```yaml
bithon:
  storage:
    providers:
      clickhouse:
        engine: your_merge_tree_engine
```
