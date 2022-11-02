## Static Configuration

Configuration in agent.yml file located at 'agent/agent-main/resources' directory

## Dynamic Configuration

Configurations via command line arguments or environment variables.
Each static configuration item has a corresponding dynamic configuration item.

Given a static configuration `agent.plugin.http.incoming.filter.uri.suffixes`, it could be set by dynamic configuration as

```bash
-Dbithon.agent.plugin.http.incoming.filter.uri.suffixes=.html
```

## Configurations

### Dispatcher Configuration

The default dispatcher configuration locates in the `agent.yml` file under `agent-distribution` module.

```yaml
dispatchers:
  metric:
    client:
      factory: org.bithon.agent.dispatcher.brpc.BrpcChannelFactory
      maxLifeTime: 300000
    servers: 127.0.0.1:9898
  tracing:
    client:
      factory: org.bithon.agent.dispatcher.brpc.BrpcChannelFactory
      maxLifeTime: 300000
    servers: 127.0.0.1:9895
    batchSize: 500
    flushTime: 3000
    queueSize: 8192
  event:
    client:
      factory: org.bithon.agent.dispatcher.brpc.BrpcChannelFactory
      maxLifeTime: 300000
    servers: 127.0.0.1:9896
    batchSize: 100
    flushTime: 5000
```

| Path               | Description                                                                                                        | Example                       | 
|--------------------|--------------------------------------------------------------------------------------------------------------------|-------------------------------|
| client.maxLifeTime | How long the client channel should be kept.                                                                        | 30000                         |
| servers            | The addresses where remote service locates. If there are multiple servers, a comma is used to split the addresses. | 127.0.0.1:9898,127.0.0.2:9898 |
| batchSize          | The max size of messages that can be sent in one batch.                                                            | 500                           |
| flushTime          | The interval of sending messages in milliseconds if there are no enough messages that can be put in one batch.     |

### Plugin Configuration

#### Agent Plugin Enabler flag

User can disable a specific plugin by passing a system property to the java application to debug or tailor unnecessary plugins.
Say we want to disable the `webserver-tomcat` plugin, passing the following property

```bash
-Dbithon.agent.plugin.webserver.tomcat.disabled=true
```

#### Agent Plugin Configuration

Plugin configuration locates each plugin's resource directory with the name 'plugin.yml'q

| configuration                                         | description                          | default | example     |
|-------------------------------------------------------|--------------------------------------|---------|-------------|
| agent.plugin.http.incoming.filter.uri.suffixes        | comma separated string in lower case |         | .html,.json |
| agent.plugin.http.incoming.filter.user-agent.matchers | A Matcher list                       |         |             |

# Tracing Configurations

| configuration                         | description                                                                                     | default | example                                |
|---------------------------------------|-------------------------------------------------------------------------------------------------|---------|----------------------------------------|
| tracing.default.samplingRate          | Percentage of incoming HTTP requests to be sampled. <br/>Value must be in the range of [0,100]. | 1       | 50(means 50% requests will be sampled) |
| tracing.brpc.samplingRate             | Percentage of BRPC requests to be sampled. <br/>Value must be in the range of [0,100].          | 1       | 50(means 50% requests will be sampled) |
| tracing.quartz.samplingRate           | Percentage of quartz jobs to be sampled. <br/>Value must be in the range of [0,100].            | 1       | 50(means 50% jobs will be sampled)     |
| tracing.spring-scheduler.samplingRate | Percentage of spring scheduled jobs to be sampled. <br/>Value must be in the range of [0,100].  | 1       | 50(means 50% jobs will be sampled)     |
| tracing.debug                         | Whether to enable the logging of span events.                                                   | false   |                                        |
| tracing.traceIdInResponse             | The header name in a HTTP response that contains the trace-id.                                  | null    |                                        |                                  

# Plugin Configurations

- [Alibaba Druid](../agent-plugin/alibaba-druid.md)
- [Spring WebFlux](../agent-plugin/spring-webflux.md)