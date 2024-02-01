# Configuration architecture

The agent supports multi-layer configurations.
1. Default configuration
2. External configuration
3. Java command line arguments
4. Environment variables
5. Dynamic configurations

The latter one has higher priority than the previous one. 
That's to say, if two same configuration keys are defined across multiple configuration sources,
the latter one will take effect.

## Default Configuration (YAML file format)

Configuration in agent.yml file located at 'agent/agent-main/resources' directory.
This static configuration is shipped with the agent distribution.

You can customize this configuration as the default configuration of your own distribution.

## External Configuration (YAML file format)

Since the default configuration customizes the global configuration of one distribution,
there are still cases that users of that particular distribution would need a configuration for a group of applications.

In such a case, the external configuration serves such a need.
Users can use `-Dbithon.configuration.location` JVM command line argument
to specify an external configuration file for the agents.

## Java command line arguments

Can be specified via the `-D` parameter in the JVM parameters sections.
Usually we use these arguments to customize two important parameters: the application name and application environment.

When using this format of configuration, all parameters must start with `-Dbithon.`.
For example:

```text
java -Dbithon.application.name=bithon-server -Dbithon.application.env=local -jar bithon-server.jar
```

## Environment variables

Configurations can also be set by environment variables. This is helpful when the agent is loaded in docker environment.

```text
export bithon_application_name=bithon-server
export bithon_application_env=local
java -jar bithon-server.jar
```

Note that all environment variables are in underscore mode.

## Dynamic Configuration

The Bithon server can store configurations for agents. We can use the API to manage(create/update/delete) configurations of an application or a group of applications.
And the agent itself will fetch configurations from the server periodically.

You can take a look at the API in the code for reference:
```
/api/agent/configuration/add
/api/agent/configuration/update
/api/agent/configuration/delete
/api/agent/configuration/get
```

# Configuration Instruction

## Dispatcher Configuration

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

## Plugin Configuration

### Agent Plugin Enabler flag

User can disable a specific plugin by passing a system property to the java application to debug or tailor unnecessary plugins.
Say we want to disable the `webserver-tomcat` plugin, passing the following property

```bash
-Dbithon.agent.plugin.webserver.tomcat.disabled=true
```

### Agent Plugin Configuration

Plugin configuration locates each plugin's resource directory with the name 'plugin.yml'

| configuration                                         | description                          | default | example     |
|-------------------------------------------------------|--------------------------------------|---------|-------------|
| agent.plugin.http.incoming.filter.uri.suffixes        | comma separated string in lower case |         | .html,.json |
| agent.plugin.http.incoming.filter.user-agent.matchers | A Matcher list                       |         |             |

# Tracing Configurations

| configuration                                               | description                                                                                                                                                                       | default           | example                                 |
|-------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|-----------------------------------------|
| tracing.samplingConfigs.default.samplingPercentage          | Percentage of incoming HTTP requests to be sampled. <br/>Zero or negative means no sampling, while 100% or above means all requests will be sampled. The minimum value is 0.001%. | 1%                | 50%(means 50% requests will be sampled) |
| tracing.samplingConfigs.brpc.samplingPercentage             | Percentage of BRPC requests to be sampled.                                                                                                                                        | 1%                | 50%(means 50% requests will be sampled) |
| tracing.samplingConfigs.quartz.samplingPercentage           | Percentage of quartz jobs to be sampled.                                                                                                                                          | 1%                | 50%(means 50% jobs will be sampled)     |
| tracing.samplingConfigs.spring-scheduler.samplingPercentage | Percentage of spring scheduled jobs to be sampled.                                                                                                                                | 1%                | 50%(means 50% jobs will be sampled)     |
| tracing.samplingConfigs.kafka-consumer.samplingPercentage   | Percentage of spring scheduled jobs to be sampled.                                                                                                                                | 1%                | 50%(means 50% jobs will be sampled)     |
| tracing.samplingConfigs.grpc.samplingPercentage             | Percentage of GRPC requests at the server side to be sampled.                                                                                                                     | 1%                | 50%(means 50% requests will be sampled) |
| tracing.disabled                                            | Whether to enable tracing.                                                                                                                                                        | false             |                                         |
| tracing.debug                                               | Whether to enable the logging of span events.                                                                                                                                     | false             |                                         |
| tracing.traceResponseHeader                                 | The header name in a HTTP response that contains the trace-id.                                                                                                                    | 'X-Bithon-Trace-' |                                         |                                  

## Plugin Configurations

- [Alibaba Druid](plugin/alibaba-druid.md)
- [Spring WebFlux](plugin/spring-webflux.md)