This doc describes the open API that interacts with the tracing module of the Bithon's agent.

## Get trace-id in your applications

Applications are able to get the trace id of current HTTP request. Bithon does not provide SDK for applications to do so but injects the trace id into the HTTP
request object which can be accessed by the application. Different web servers have different types of HTTP objects, so the APIs to get the trace id may vary.

| Web Server | Class                                                      | Method                                                 |
|------------|------------------------------------------------------------|--------------------------------------------------------|
| Tomcat     | javax.servlet.http.HttpServletRequest                      | getAttribute("X-Bithon-TraceId")                       |
| Jetty      | javax.servlet.http.HttpServletRequest                      | getAttribute("X-Bithon-TraceId")                       |
| Undertow   | javax.servlet.http.HttpServletRequest                      | getAttribute("X-Bithon-TraceId")                       |
| WebFlux    | org.springframework.http.server.reactive.ServerHttpRequest | getRequest().getHeaders().getFirst("X-Bithon-TraceId") |

If the target method call returns non-null, it means current request is being sampled under the returned trace id.

### Example

#### Tomcat/Jetty/Undertow WebServers

```java

@RestController
public class TestController {
    //
    // get trace id in the controller
    //
    @GetMapping("/api/test")
    public void test(javax.servlet.http.HttpServletRequest request) {
        System.out.println((String) request.getAttribute("X-Bithon-TraceId"));
    }
}
```

#### WebFlux Server

```java

@RestController
public class TestController {
    //
    // get trace id in the controller
    //
    @GetMapping("/api/test")
    public void test(org.springframework.http.server.reactive.ServerHttpRequest request) {
        System.out.println(request.getHeaders().getFirst("X-Bithon-TraceId"));
    }
}

//
// get trace id inside the gateway filter
//
public class TestFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Bithon-TraceId");
        System.out.println(traceId);

        return chain.filter(exchange);
    }
}
```

> NOTE
>
> Modification or deletion to the trace id above will NOT change the trace id of current tracing logs.
>
