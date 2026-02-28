
## Nacos

The servers support Alibaba Nacos as a configuration and service discovery center. 

> Current supported nacos server is 2.3.1. 
> 
> If your nacos sever is not compatible with this version, 
> you can change the nacos client dependency in server/server-starter/pom.xml to match your nacos server.
> 

## Enable Nacos

On Spring Boot 4 / Spring Cloud Alibaba 2025.x, Nacos config must be imported from `application.yml`.
The old bootstrap-based settings are not enough by themselves anymore.

The starter now imports the base Nacos DataId from `server/server-starter/src/main/resources/application.yml`.
Profile-specific Nacos DataIds should also be declared in `application.yml` by using a profile-activated
document with `spring.config.activate.on-profile`.

You can enable the Nacos config client by setting `spring.cloud.nacos.config.enabled` to `true` as follows.

```yaml
spring:
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml
  cloud:
    nacos:
      config:
        enabled: true
```

Example for the `prod` profile:

```yaml
---
spring:
  config:
    activate:
      on-profile: prod
    import:
      - optional:nacos:${spring.application.name}-prod.yaml
```

You also need to change the following nacos configuration items to reflect the items in nacos server.

```yaml
spring:
  cloud:
    nacos:
      config:
        file-extension: yaml
        server-addr: nacos.server:80
        namespace: ffffffff-d350-4755-ab1a-28721fc79cfa
        refresh-enabled: true
        group: DEFAULT_GROUP
```

## Enable Service Discovery

If the collector is deployed with web-server separately, we also need to enable service discovery.
To do this, we need to turn on `spring.cloud.nacos.discovery.enabled`, and set correct Nacos server address by property `spring.cloud.nacos.discovery.server-addr`.
Here's an example.

```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: true
        watch:
          enabled: false
        server-addr: nacos.server:80
```
