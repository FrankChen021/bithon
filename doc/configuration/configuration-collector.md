
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

```yaml
collector-brpc:
  enabled: true
  port:
    tracing: 9895
    event: 9896
    metric: 9898
    ctrl: 9899
  sink:
    type: kafka
    props:
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

```yaml
collector-kafka:
  enabled: true
  source:
    "[bootstrap.servers]": localhost:9092
    "[fetch.min.bytes]": 1024
    ...other Kafka Consumer Properties
```