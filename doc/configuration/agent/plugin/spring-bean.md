
The Spring Bean plugin is responsible for instrumenting Spring Beans in a Spring Boot application.
All beans will be instrumented by default.
This might bring in some performance penalties during the application startup.

It provides some configurations to control the instrumentation.

Again, you can use any of the methods mentioned in the [agent configuration](../README.md) doc to control these configurations.
In this doc, we use the JVM system property to change configurations as examples.

### Scenario 1 - Disable the plugin completed

You can disable this plugin by setting the `enabled` configuration to `false`.

```text
-Dbithon.agent.plugin.spring.bean.enabled=false
```

### Scenario 2 - Only instrument beans annotated

If you only want to instrument beans that are annotated with Spring `@Service`,
`@Component`, `@Repository`, you can use the following configuration.

```text
-Dbithon.agent.plugin.spring.bean.enableServiceComponentOnly=true
```

### Scenario 3 - Exclude specific beans

You can exclude specific beans from being instrumented by providing a list of bean names.

Please see the [YAML configurations](../../../../agent/agent-plugins/spring-bean/src/main/resources/org.bithon.agent.plugin.spring.bean.yml) under this plugin for reference.
