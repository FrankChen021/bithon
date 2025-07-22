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

> NOTE: Variable replacement in the configuration is not supported. For example:
> 
> ```
> application:
>   name: ${ENV:name}
> ```
> 
> The value of the `application.name` property will not be replaced but the right `{ENV:name}`. 

## External Configuration (YAML file format)

Since the default configuration customizes the global configuration of one distribution,
there are still cases that users of that particular distribution would need a configuration for a group of applications.

In such a case, the external configuration serves such a need.
Users can use `-Dbithon.configuration.location` JVM command line argument
to specify an external configuration file for the agents.

> NOTE: Variable replacement in the configuration is not supported.

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

The Bithon server can store configurations for agents. We can use the API to manage(add/update/delete/get) configurations of an specified target application or a group of target applications.

The agent running at the application side will automatically fetch configurations from the server periodically and merge them with above configurations.

Please refer to the [Dynamic Configuration API](../../api/dynamic-configuration-api.md) document to use API to manage dynamic configurations.

# Configuration Instruction

## Basic Configuration (Must read)

### Configure the application name (Needed)
There are two configurations that need to be provided to the agent, corresponding JVM arguments are:

```
-Dbithon.application.name=
-Dbithon.application.env=
```

> NOTE: you can also use environment variable `bithon_application_name` and `bithon_application_env` to configure these two configurations as stated in the Environment Variables section.

### Configure the instance name (Optional)
The agent automatically uses the IP address of the environment
that the application runs on to generate the `instanceName` field in metrics/tracing/events message.

If you want to specify the instance name by your custom rule, you can also configure it by the following JVM argument

```
-Dbithon.application.instance=
```

Remember that the configured value should be unique for each running application instance
or you're not able to drill down metrics by the instance name.

### Configure the port (Optional)

By default, the agent will automatically detect the port that the web server inside the application runs on.
And this port will be used as part of the instance name along with the IP address or the `-Dbithon.application.instance` property. 

If you want to specify the application port, you can also use the following JVM parameter to configure:

```
-Dbithon.application.port=
```

### Use the pod name in K8S as the instance name (Recommended)
In K8S, we can use the POD name as the instance name(`bithon.application.instance`)
so that it would be much easier to find out metrics of one container by the POD name instead of by the IP address of a POD.

To do this, we can add environment variable `bithon_application_instance` in the K8S configuration to be value of the pod name as follows:

```yaml
 env:
  - name: bithon_application_instance
    valueFrom:
      fieldRef:
        fieldPath: metadata.name
```

## Dispatcher Configuration

The default exporter configuration locates in the `agent.yml` file under `agent-distribution` module.

```yaml
exporters:
  metric:
    client:
      factory: org.bithon.agent.exporter.brpc.BrpcChannelFactory
      maxLifeTime: 300000
    servers: 127.0.0.1:9898
  tracing:
    client:
      factory: org.bithon.agent.exporter.brpc.BrpcChannelFactory
      maxLifeTime: 300000
    servers: 127.0.0.1:9895
    batchSize: 500
    flushTime: 3000
    queueSize: 8192
  event:
    client:
      factory: org.bithon.agent.exporter.brpc.BrpcChannelFactory
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
We can use the following property format to disable it:

```bash
-Dbithon.agent.plugin.[plugin-name].disabled=true
```
The `plugin-name` can be found from the following list:

* alibaba.druid
* apache.druid
* apache.kafka
* apache.ozone
* bithon.brpc
* bithon.sdk
* glassfish
* grpc
* guice
* httpclient.apache
* httpclient.jdk
* httpclient.jetty
* httpclient.netty3
* httpclient.okhttp32
* jedis
* jersey
* jetty
* lettuce
* log4j2
* logback
* mongodb
* mongodb38
* mysql
* mysql8
* netty
* netty4
* quartz2
* spring.bean
* spring.boot
* spring.mvc
* spring.scheduling
* spring.webflux
* thread
* tomcat
* undertow
* xxl.job

For example, if we want to disable the `tomcat` HTTP server plugin, passing the following property

```bash
-Dbithon.agent.plugin.webserver.tomcat.disabled=true
```

Once the application starts, a log will be printed to show the status of this plugin:

```text
15:38:01.155 [main] INFO org.bithon.agent.instrumentation.aop.interceptor.plugin.PluginResolver - Found plugin [tomcat], but it's DISABLED by configuration
```

### Agent Plugin Configuration

Plugin configuration locates each plugin's resource directory with the name 'plugin.yml'

| configuration                                         | description                          | default | example     |
|-------------------------------------------------------|--------------------------------------|---------|-------------|
| agent.plugin.http.incoming.filter.uri.suffixes        | comma separated string in lower case |         | .html,.json |
| agent.plugin.http.incoming.filter.user-agent.matchers | A Matcher list                       |         |             |

# Tracing Configurations

| configuration                                               | description                                                                                                                                                                                | default           | example                                 |
|-------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|-----------------------------------------|
| tracing.samplingConfigs.default.samplingPercentage          | Percentage of incoming HTTP requests to be sampled. <br/>Zero or negative means no sampling, while 100% or above means all requests will be sampled. The minimum and step value is 0.001%. | 100%              | 50%(means 50% requests will be sampled) |
| tracing.samplingConfigs.brpc.samplingPercentage             | Percentage of BRPC requests to be sampled.                                                                                                                                                 | 1%                | 50%(means 50% requests will be sampled) |
| tracing.samplingConfigs.quartz.samplingPercentage           | Percentage of quartz jobs to be sampled.                                                                                                                                                   | 1%                | 50%(means 50% jobs will be sampled)     |
| tracing.samplingConfigs.spring-scheduler.samplingPercentage | Percentage of spring scheduled jobs to be sampled.                                                                                                                                         | 1%                | 50%(means 50% jobs will be sampled)     |
| tracing.samplingConfigs.kafka-consumer.samplingPercentage   | Percentage of Kafka consumer tasks to be sampled.                                                                                                                                         | 0.1%              | 50%(means 50% jobs will be sampled)     |
| tracing.samplingConfigs.grpc.samplingPercentage             | Percentage of GRPC requests at the server side to be sampled.                                                                                                                              | 1%                | 50%(means 50% requests will be sampled) |
| tracing.disabled                                            | Whether to enable tracing.                                                                                                                                                                 | false             |                                         |
| tracing.debug                                               | Whether to enable the logging of span events.                                                                                                                                              | false             |                                         |
| tracing.traceResponseHeader                                 | The header name in a HTTP response that contains the trace-id.                                                                                                                             | 'X-Bithon-Trace-' |                                         |
| tracing.traceIdGenerator                                    | The algorithm that is used to generate trace id. <br/> See <code>org.bithon.agent.observability.tracing.id.ITraceIdGenerator</code> to know more.                                          | 'uuidv7'          |                                         |

## Plugin Configurations


### Disable/Enable plugin

All plugins are enabled by default. They can be disabled separately. Use the following pattern to disable one plugin.

```
-Dbithon.agent.plugin.<plugin name>.disabled=true
```

For example, to disable the `spring-bean` plugin, use
```
-Dbithon.agent.plugin.spring.bean.disabled=true
```

### Plugin Configuration

- [Spring WebFlux](plugin/spring-webflux.md)
