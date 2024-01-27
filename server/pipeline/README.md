This module is responsible for the data processing.
It accepts data from a receiver and then applies processors/transformers to the data,
and then export data to downstream.

The receiver currently can be the Bithon's brpc collector or a Kafka cluster.
The exporter now can be either a persistent storage or a Kafka.

Typically, in production, we deploy two pipelines as two applications:
1. brpc collector with Kafka. The collector receives data and then push data to a Kafka cluster
2. The other pipeline consumes data from above Kafka, and then push the data to persistent storages.

For local development,
we can deploy one pipeline that consumes data from the brpc collector and then store them in storages.