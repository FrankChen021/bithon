## BRPC Collector

```yaml
collector-brpc:
  enabled: true
  port:
    tracing: 9895
    event: 9896
    metric: 9898
    ctrl: 9899
  sink:
    type: local
```

> Note:
> 1. tracing,event,metric,ctrl can share one same port

## HTTP Collector

HTTP Collector can exist with BRPC collector. It provides a separate HTTP endpoint to receive metrics and tracing messages.
To Enable the HTTP collector, configure it as follows:

```yaml
collector-http:
  enabled: true
  tracing:
    clickhouseApplications:
      - clickhouse
```

## Kafka Collector

Usually Kafka Collector is used with BRPC collector which writes messages to a Kafka cluster.
So, we typically configure the BRPC to sink to a Kafka cluster,
and then enable the Kafka collector to receive messages from that Kafka cluster.

In this case, the Kafka Collector can run with the BRPC collector in one same process or in different process.

### Deployment

1. BRPC collector and Kafka collector runs in different applications.

   This is recommended deployment mode because Kafka decouples the collectors for the application side from the backend message processing.

    ```mermaid
    flowchart LR
        client-applications --metric/tracing/events--> Bithon-BRPC-collector
        subgraph Application1
            Bithon-BRPC-collector
        end
        Bithon-BRPC-collector ---> Kafka
        subgraph Application2
            Bithon-Kafka-collector
        end
        Kafka ---> Bithon-Kafka-collector
    ```

2. BRPC collector and Kafka collector runs in a same application.

    ```mermaid
        flowchart LR
            client-applications --metric/tracing/events--> Bithon-BRPC-collector
            subgraph Application1
                Bithon-BRPC-collector
                Bithon-Kafka-collector
            end
            Bithon-BRPC-collector ---> Kafka
            Kafka ---> Bithon-Kafka-collector
    ```

### Configuration

Following configurations allows the brpc-collector sink messages to a Kafka cluster.

```yaml
collector-brpc:
  enabled: true
  port:
    tracing: 9895
    event: 9896
    metric: 9898
    ctrl: 9899
  sinks:
    event:
      type: kafka
      props:
        topic: bithon-events
        "[bootstrap.servers]": localhost:9092
        "[batch.size]": 65536
        "[buffer.memory]": 67108864
        "[linger.ms]": 50
        "[compression.type]": lz4
        "[max.in.flight.requests.per.connection]": 1
        "[retries]": 3
    metrics:
      type: kafka
      props:
        topic: bithon-metrics
        "[bootstrap.servers]": localhost:9092
        "[batch.size]": 65536
        "[buffer.memory]": 67108864
        "[linger.ms]": 50
        "[compression.type]": lz4
        "[max.in.flight.requests.per.connection]": 1
        "[retries]": 3
    tracing:
      type: kafka
      props:
        topic: bithon-spans
        "[bootstrap.servers]": localhost:9092
        "[batch.size]": 65536
        "[buffer.memory]": 67108864
        "[linger.ms]": 50
        "[compression.type]": lz4
        "[max.in.flight.requests.per.connection]": 1
        "[retries]": 3
```

> Note:
> Under `collector-brpc.sink.props` configuration path, you can add other Kafka broker properties.

And following configurations configure the application to consume messages from a Kafka cluster.

```yaml
collector-kafka:
  enabled: true
  metrics:
    topic: bithon-metrics
    concurrency: 1
    pollTimeout: 1000
    ackTime: 5000
    "[group.id]": bithon-metrics-consumer
    "[bootstrap.servers]": localhost:9092
    "[fetch.min.bytes]": 524288
    #...other Kafka consumer properties
  event:
    topic: bithon-events
    concurrency: 1
    pollTimeout: 1000
    ackTime: 5000
    "[group.id]": bithon-events-consumer
    "[bootstrap.servers]": localhost:9092
    "[fetch.min.bytes]": 1048576
    #...other Kafka consumer properties
  tracing:
    topic: bithon-spans
    concurrency: 1
    pollTimeout: 1000
    ackTime: 5000
    "[group.id]": bithon-spans-consumer
    "[bootstrap.servers]": localhost:9092
    "[fetch.min.bytes]": 1048576
    #...other Kafka consumer properties
```

Note: Both `pollTimeout` and `ackTime` are in milliseconds.

## Permission Control for UPDATE commands those are sent to agent

Example:

```yaml
collector-controller:
  permission:
    rules:
      - application:
          type: startwith
          pattern: bithon-
        token: "525"
```

Each rule matches a target application. For more information of application matcher, see the source code [IMatcher.java](../../../server/server-commons/src/main/java/org/bithon/server/commons/matcher/IMatcher.java).
