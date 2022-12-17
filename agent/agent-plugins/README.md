
## Specification

### Plugin

- The artifact name of each plugin defined in the `pom.xml` must be in the form of `agent-plugin-xxx`.

### Plugin class

- Each plugin must provide a class that implements `org.bithon.agent.core.plugin.IPlugin` interface.
- The name of each plugin class must end with `Plugin`, e.g `HttpClientPlugin`.
- Ot 

### Dependencies

The scope of dependencies declared in the `pom.xml` of each plugin must be defined as `provided`.

#### Declaration

- Each plugin must be defined in the `pom.xml` of `agent-plugins` as a module.
- Each plugin must be declared in the `<dependencies>` section so that they can be packed correctly in the distribution.
