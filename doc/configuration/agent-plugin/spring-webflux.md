
## Example Configurations

```yml
agent:
  plugin:
    spring:
      webflux:
        response:
          headers:
            HEADER_NAME: TAG_NAME_IN_SPAN
        gateway:
          org.springframework.cloud.gateway.filter.AdaptCachedBodyGlobalFilter:
            mode: before
          org.springframework.cloud.gateway.filter.NettyRoutingFilter:
            mode: around
```

- agent.plugin.spring.webflux.gateway

    This configuration configures users' Spring Gateway Filter classes to be instrumented

- agent.plugin.spring.webflux.response.headers

    A map that specifies which headers in the HTTP Response should be logged in the `tags` of span log.
    
    - key, the HEADER name in the HTTP response.
    - val, the tag name in the span log.
