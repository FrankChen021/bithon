
## Nacos

The servers support Alibaba Nacos as a configuration and service discovery center. 

> Current supported nacos server is 2.3.1. 
> 
> If your nacos sever is not compatible with this version, 
> you can change the nacos client dependency in server/server-starter/pom.xml to match your nacos server.
> 

## Enable Nacos

By default, Nacos is not enabled in `server/server-starter/src/main/resources/bootstrap.yml`.

You can enable it by change the `spring.cloud.nacos.config.enabled` from `false` to `true` as following.

```yaml
spring:
  cloud:
    nacos:
      config:
        enabled: false
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