[![Build Status](https://app.travis-ci.com/FrankChen021/bithon.svg?branch=master)](https://app.travis-ci.com/github/FrankChen021/bithon)

---

# Introduction

Bithon is a word combining binocular together with python.

It targets application metrics, logging, distributed tracing, alert and application risk governance under microservice environment.

## Architecture

![architecture.png](doc/intro/architecture.png)

The above pic illustrates the main components of this project, including:
- Agent, which collects metrics/tracing logs automatically from client application without any code modification at the application side
- Collector, which provides various interfaces (including OpenTelemetry GRPC interface) to receive metrics/tracing logs from clients
- Pipeline, which provides a flexible and robust way to hande small data scale to an extra huge data scale for incoming metrics or tracing logs
- Storage, which provides an abstraction to underlying storages like H2, MySQL or Clickhouse
- Alerting, which allows us to set up alerts by using MetricSQL style expression on existing metrics or tracing logs and data in external storages
- Web, which provides NextJS-based web page for metrics/tracing/alerting visualization 

## Highlights

- **Comprehensive Metrics**
  - Over 200 built-in metrics covering JDK internals and popular Java middlewares such as Apache Http Components.
- **Advanced Distributed Tracing**
  - Seamless compatibility with major tracing context propagation formats, including:
    - OpenTelemetry
    - Zipkin
    - Jaeger
    - Pinpoint
  - Multi-channel span log ingestion:
    - OpenTelemetry via GRPC or HTTP
    - Zipkin via HTTP
    - Jaeger Thrift via HTTP or UDP

- **Intelligent Logging**
  - Automatic injection of traceId/spanId into logs, requiring zero code changes in client applications.
  - Trace context enrichment even when distributed tracing is disabled.

- **Deep Profiling & Diagnostics**
  - Real-time JMX bean inspection for target applications.
  - Live and continuous thread dump analysis.
  - Continuous CPU and memory profiling for in-depth performance insights.

- **Powerful Alerting**
  - PromQL-style alerting expressions for flexible, real-time monitoring and incident response.

- **Flexible Deployment**
  - Adaptable architecture for both small-scale and massive-scale data environments.
    - Single all-in-one JAR for quick evaluation and lightweight use cases.
    - Modular, component-based deployment for huge-scale scenarios.
  - Effortless horizontal scaling to handle over 100TiB of data.

- **Exceptional Cost Efficiency**
  - Ultra-fast queries and minimal storage overhead, powered by ClickHouse integration.

# Preview

You can use the [docker-compose.yml](docker/docker-compose.yml) to start the whole system for preview.

```bash
docker-compose -f docker/docker-compose.yml up
```

Once all services in the docker-compose are up, you can visit http://localhost:9900 to access UI.
And by default, the application itself is configured to be self-monitored, you will see the metrics/tracing of the application itself.



## Demo
A demo is provided by this [demo repo](https://github.com/FrankChen021/bithon-demo) with a docker-compose file.
You can follow the README on that demo repo to start the demo within just 3 steps.

# Build

## 1. Clone source code

After cloning this project along with all submodules by following commands

```bash
git clone https://github.com/FrankChen021/bithon.git
cd bithon && git submodule update --init
```

## 2. Configure JDK

JDK 17 and above are required to build this project.
If you have multiple JDKs on your machine, use `export JAVA_HOME={YOUR_JDK_HOME}` command to set correct JDK. 
For example

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home
```

## 3. Build the project

For the first time to build this project, use the following command to build dependencies first: 

```bash
mvn clean install --activate-profiles shaded,jooq -T 1C
```

and then execute the following command to build the project. 

```bash
mvn clean install -DskipTests -T 1C
```

After the first build, we don't need to build the dependencies anymore unless there are changes in these dependencies.

# Run

Once the project has been built, you could run the project in a standalone mode to evaluate this project.

## 1. Launch the backend services all in one

To launch server in evaluation mode, execute the following command:

```bash
java -Dspring.profiles.active=all-in-one -jar server/server-starter/target/server-starter.jar
```

By default, the application opens and listens on following ports at local

| Function | Port |
|----------|------|
| tracing  | 9895 |
| event    | 9896 |
| metric   | 9898 |
| ctrl     | 9899 |
| web      | 9897 |

> Note:
> `-Dspring.profiles.include` parameter here is just for demo.
> 
> You can make changes to `server/server-starter/src/main/resources/application.yml` to reflect your own settings.
> 
> You can also use enable [Alibaba Nacos](doc/configuration/server/configuration-nacos.md) as your configuration storage center.

## 2. Attach agent to your target Java applications

Attach the agent to your java application so that your application can be managed the agent.
Add the following VM arguments to your target Java application.

```bash
-javaagent:<YOUR_PROJECT_DIRECTORY>/agent/agent-distribution/target/agent-distribution/agent-main.jar -Dbithon.application.name=<YOUR_APPLICATION_NAME> -Dbithon.application.env=<YOUR_APPLICATION_ENV>
```

| Variable               | Description                                                                                                              |
|------------------------|--------------------------------------------------------------------------------------------------------------------------|
| YOUR_PROJECT_DIRECTORY | the directory where this project saves                                                                                   |
| YOUR_APPLICATION_NAME  | the name of your application. It could be any string                                                                     |
| YOUR_APPLICATION_ENV   | the name of your environment to label your application. It could be any string. Usually it could be `dev`, `test`, `prd` |

By default, the agent connects collector running at local(127.0.0.1). 
Collector address could be changed in file `agent/agent-main/src/main/resources/agent.yml`.
Make sure to re-build the project after changing the configuration file above.

> For production deployment, please refer to the [agent deployment doc](doc/deployment/agent-deployment.md) to deploy the agent to your target Java applications.

# JDKs Compatibility

Even the project is built by JDK 17 and above, the agent is compatible with JDK 1.8+.
The following matrix lists the JDKs that are compatible with the agent on macOS. 
And in theory, this matrix works both for Windows and Linux.

| JDK           | Supported | 
|---------------|-----------|
| JDK 1.8.0_291 | &check;   |
| JDK 9.0.4     | &check;   |
| JDK 10.0.2    | &check;   |
| JDK 11.0.12   | &check;   |
| JDK 12.0.2    | &check;   |
| JDK 13.0.2    | &check;   |
| JDK 14.0.2    | &check;   |
| JDK 15.0.2    | &check;   |
| JDK 16.02     | &check;   |
| JDK 17        | &check;   |
| JDK 21        | &check;   |
| JDK 22.0.2    | &check;   |
| JDK 23.0.2    | &check;   |
| JDK 24        | &check;   |

> NOTE:
> For applications running on JDK 24, the agent may not work properly to handle tracing across threads.


# Supported Components

| Component                                                    | Min Version | Max Version | Metrics                                        | Tracing |
|--------------------------------------------------------------|-------------|-------------|------------------------------------------------|---------|
| JVM                                                          | 1.8         |             | &check;                                        |         |
| JDK - Thread Pool                                            | 1.8         |             | &check;                                        | &check; |
| JDK - HTTP Client                                            | 1.8         |             | [&check;](doc/metrics/http-outgoing/README.md) | &check; |
| Apache Druid(1)                                              | 0.16        | 31.0        |                                                | &check; |
| Apache Kafka(2)                                              | 0.10.0.0    | 4.0         | &check;                                        | &check; |
| Apache OZone                                                 | 1.3.0       |             |                                                | &check; |
| Apache ZooKeeper Client                                      | 3.5         | 3.9         | &check;                                        |         |
| Eclipse Glassfish                                            | 2.34        |             |                                                | &check; |
| GRPC                                                         | 1.57.0      |             | &check;                                        | &check; |
| Google Guice                                                 | 4.1.0       |             |                                                | &check; |
| HTTP Client - Apache                                         | 4.5.2       | 5.x         | [&check;](doc/metrics/http-outgoing/README.md) | &check; |
| HTTP Client - Jetty                                          | 9.4.6       |             | [&check;](doc/metrics/http-outgoing/README.md) | &check; |
| HTTP Client - Netty                                          | 3.10.6      | < 4.0       | [&check;](doc/metrics/http-outgoing/README.md) | &check; |
| HTTP Client - okhttp3                                        | 3.2         | 4.9         | [&check;](doc/metrics/http-outgoing/README.md) | &check; |
| HTTP Client - reactor-netty                                  | 1.0.11      |             | [&check;](doc/metrics/http-outgoing/README.md) | &check; |
| HTTP Client - JDK HttpURLConnection                          | 1.8         |             | [&check;](doc/metrics/http-outgoing/README.md) | &check; |
| HTTP Client - JDK 11+ HttpClient (java.net.http.HttpClient)  | 11          |             | [&check;](doc/metrics/http-outgoing/README.md) | &check; |
| Jersey                                                       | 1.19.4      |             |                                                | &check; |
| JDBC - Alibaba Druid                                         | 1.0.28      |             | &check;                                        | &check; |
| JDBC - Apache Derby                                          | 10.14.2     |             | &check;                                        | &check; |
| JDBC - ClickHouse                                            | 0.3.1       |             | &check;                                        | &check; |
| JDBC - H2                                                    | 2.2.224     |             | &check;                                        | &check; |
| JDBC - MySQL                                                 | 5.x         | 8.x         | &check;                                        | &check; |
| JDBC - PostgreSQL                                            | 42.4.3      |             | &check;                                        | &check; |
| MongoDB                                                      | 3.4.2       |             | &check;                                        |         |
| Open Feign                                                   | 10.8        |             |                                                | &check; |
| Quartz                                                       | 2.x         |             | &check;                                        | &check; |
| Redis - Jedis                                                | 2.9         | 5.x         | &check;                                        | &check; |
| Redis - Lettuce(3)                                           | 5.1.2       | 6.x         | &check;                                        | &check; |
| Redis - Redisson                                             | 3.19.0      |             | &check;                                        | &check; |
| Spring Boot                                                  | 1.5         | 3.0+        |                                                | &check; |
| [Spring Bean](doc/configuration/agent/plugin/spring-bean.md) | 4.3.12      |             |                                                | &check; |
| Spring Open Feign                                            | 10.8        |             |                                                | &check; |
| Spring Rest Template                                         | 4.3.12      |             |                                                | &check; |
| Spring Scheduling                                            | 4.3.12      |             |                                                | &check; |
| Spring Gateway                                               | 3.0.0       |             | [&check;](doc/metrics/http-outgoing/README.md) | &check; | 
| HTTP Server - Jetty                                          | 9.4.41      | 12.0.x      | &check;                                        | &check; |
| HTTP Server - Netty                                          | 2.0.0       |             |                                                | &check; |
| HTTP Server - Tomcat                                         | 8.5.20      |             | &check;                                        | &check; |
| HTTP Server - Undertow                                       | 1.4.12      |             | &check;                                        | &check; |
| xxl-job                                                      | 2.3.0       |             |                                                | &check; |

## Restrictions
1. For Apache Druid, the Jersey plugin is required to be enabled to collect query information.
2. From Apache Kafka clients 3.7, the consumer metrics only works when the `group.protocol` is configured as `classic` which is the default configuration of the consumer client. 
3. For Lettuce, the tracing support is only available when it's used with Spring Data Redis API(org.springframework.data:spring-data-redis) from 2.3.4-RELEASE .




# User Doc
1. [Metrics](doc/metrics/README.md)
2. [Tracing](doc/tracing/README.md)
3. [Logging](doc/logging/README.md)
4. [Diagnosis](doc/diagnosis/README.md)
5. [Configuration](doc/configuration/configuration.md)
6. SDK
   1. [Metrics](doc/sdk/metrics.md)
   2. [Tracing](doc/sdk/tracing.md)

# Contribution

To develop for this project, intellij is recommended.

A code style template file(`dev/bithon_intellij_code_style`) must be imported into intellij for coding.

For more information, check the [development doc](doc/dev/development.md).

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
