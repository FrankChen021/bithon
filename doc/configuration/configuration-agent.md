
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

| configuration        | description                                                                       | default | example                                |
|----------------------|-----------------------------------------------------------------------------------|---------|----------------------------------------|
| tracing.samplingRate | percentage of requests to be sampled. <br/>Value must be in the range of [0,100]. | 0       | 50(means 50% requests will be sampled) |


# Plugin Configurations

- [Alibaba Druid](agent-plugin/jdbc-druid.md)
- [Spring WebFlux](agent-plugin/spring-webflux.md)