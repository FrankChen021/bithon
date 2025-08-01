This doc describes the open API that interacts with the tracing module of the Bithon's agent.

## Usage of Tracing SDK

The Tracing SDK is designed to be easy to use, allowing developers to create spans with minimal effort.

To make it work, the Bithon agent MUST be installed so that the SDK can function properly. 
The SDK is designed to be easy to use, allowing developers to create spans with minimal effort.

### Step.1: Add Tracing SDK dependency
Add the Bithon Tracing API dependency to your project. The dependency is available in Maven Central.

```xml
<dependency>
    <groupId>org.bithon.agent</groupId>
    <artifactId>agent-sdk</artifactId>
    <version>1.2.1</version>
</dependency>
```

> NOTE:
> 
> 1.2.0 is the current release version. You can find the latest version in [Maven Central](https://search.maven.org/artifact/org.bithon.agent/agent-sdk).

### Step.2: Create a trace span

The following code shows how to create a trace span in your application.

```java
@RestController
public class UserApi {
    /*
     * The following code is from the bithon-demo project which is available at https://github.com/FrankChen021/bithon-demo
     */
    @PostMapping("/api/user/register")
    public RegisterUserResponse register(@RequestBody RegisterUserRequest request) {
        Long uid;
        try (ISpan span = TraceContext.newScopedSpan()) {
            span.name("dao#create").start();
            
            uid = userDao.create(request.getUserName(), request.getPassword());
            if (uid == null) {
                return RegisterUserResponse.builder().error(String.format("User [%s] exists.", request.getUserName())).build();
            }
        }
        
        logService.addLog(request.getUserName(), "REGISTER");
        
        try (ISpan span = TraceContext.newScopedSpan()) {
            span.name("event#publish").start();
            eventPublisher.publishEvent("REGISTER");
        }

        return RegisterUserResponse.builder().uid(uid.toString()).build();
    }
}
```

> NOTE:
> 1. Before the TraceContext.newScopedSpan() is called, the Bithon agent must be installed and started, and current request is SAMPLED.
> 
>   If the agent is not loaded, the API will return NoopSpan which does nothing, and logs 'The agent is not loaded.' to your application logs.
>
> 2. MAKE SURE the span is **closed** after it is used, otherwise the span or spans of current traced request will **NOT** be sent to the server. 
> 3. Putting the span in a try-with-resources block is a good practice. 
> 

### API list
| API Name                                           | Description                                                                                                               |
|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| org.bithon.agent.sdk.TraceContext#newScopedSpan()  | Creates a new span. The span is automatically closed when the method exits.                                               |
| org.bithon.agent.sdk.TraceContext#currentTraceId() | Get trace id for current request. If the tracing is NOT enabled or current request is NOT sampled, null will be returned. |


### Understand how the tracing SDK works

The tracing SDK can be seen as a wrapper around the Bithon agent. In its core, it does NOTHING if you step in the code of it.

During the runtime, when the Bithon agent is loaded, the agent will automatically replace the implementation of all methods in the SDK to delegate the calls to the agent.

If you want to know more, please refer to the [bithon-sdk](../../agent/agent-plugins/bithon-sdk) module.

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
