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
> 1.2.1 is the current release version. You can find the latest version in [Maven Central](https://search.maven.org/artifact/org.bithon.agent/agent-sdk).

### Step.2: Create trace spans

The Bithon SDK provides two main approaches for creating trace spans:

#### 2.1 Creating Individual Spans with `newScopedSpan()`

Use `TraceContext.newScopedSpan()` to create individual spans within an existing tracing context.

The **existing tracing context** here means, it can be a context created by the agent automatically (like for HTTP endpoints) or created manually using the API shown in section [2.2](#22-creating-root-trace-scopes-with-newtrace).

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

            // The business logic code
            uid = userDao.create(request.getUserName(), request.getPassword());
            if (uid == null) {
                return RegisterUserResponse.builder().error(String.format("User [%s] exists.", request.getUserName())).build();
            }
        }
        
        logService.addLog(request.getUserName(), "REGISTER");
        
        try (ISpan span = TraceContext.newScopedSpan()) {
            span.name("event#publish")
                // Can use the 'tag' method to record custom information into the tracing logs
                .tag("user", request.getUserName())
                .start();

            // The business logic code
            eventPublisher.publishEvent("REGISTER");
        }

        return RegisterUserResponse.builder().uid(uid.toString()).build();
    }
}
```

#### 2.2 Creating Root Trace Scopes with `newTrace()`

Use `TraceContext.newTrace()` to create a new root trace scope.
This is useful when you want to control how the `traceId` is generated, or you want to set up tracing context for asynchronous processing inside your application.

> NOTE
> 
> When using this API to create a tracing context, the context is going to be bounded to the thread where the `attach` method is called. 
> Callers must ensure that there's no tracing context(whether it's an automatically created context by the agent or by previous call of this API) attached to that thread or an exception will be thrown.
> 
>

The following examples demonstrates the usage of this API in two scenarios:
1. Set up a tracing context on a background task
2. Propogate the tracing context for asynchronous processing by using of `java.lang.Thread`

```java
@Service
public class BackgroundTaskService {
    
    // Simple root trace
    public void processTask(String taskId) {
        try (ITraceScope scope = TraceContext.newTrace("process-task").attach()) {
            // Get current span and add metadata
            ISpan span = scope.currentSpan();
            span.tag("task.id", taskId)
                .tag("task.type", "background");
                
            // Business logic here
            performTaskProcessing(taskId);
        }
    }
    
    // Root trace with custom configuration
    public void processHighPriorityTask(String taskId) {
        try (ITraceScope scope = TraceContext.newTrace("high-priority-task")
                .kind(SpanKind.INTERNAL)
                .tracingMode(TracingMode.TRACING)
                .attach()) {
                
            ISpan span = scope.currentSpan();
            span.tag("priority", "high")
                .tag("task.id", taskId);
                
            // Start a thread for async processing
            scheduleAsyncTask(taskId);
        }
    }
    
    // Cross-thread trace continuation
    private void scheduleAsyncTask(String taskId) {
        // When running here, we're still in the SAME thread where processHighPriorityTask is running, 
        // So we can access the following method to capture current traceId and parentSpanId for tracing context propagation.
        String traceId = TraceContext.currentTraceId();
        String parentSpanId = TraceContext.currentSpanId();
        
        // Schedule task in another thread
        new Thread(() -> {
            try (ITraceScope asyncScope = TraceContext.newTrace("async-task")
                    // Set up the tracing context with capture traceId and parentSpanId
                    .parent(traceId, parentSpanId)
                    .kind(SpanKind.INTERNAL)
                    .attach()) {
                    
                ISpan span = asyncScope.currentSpan();
                span.tag("task.id", taskId)
                    .tag("execution.type", "async");
                    
                // Async business logic here
                ...
            }
        }).start();
    }
}
```

### 2.3 Important Notes

1. All the tracing APIs can only work with the bithon agent. Before you run the application, make sure the agent has been loaded.
 
     If the agent is not loaded, the APIs will return no-op implementations and log 'The agent is not loaded.' to your application logs.

2. **ALWAYS** ensure spans are closed after use, otherwise spans will **NOT** be sent to the server. Using try-with-resources blocks is the recommended practice.
 
3. `newScopedSpan()` creates child spans within the current tracing context, while `newTrace()` creates new root trace scopes.
 
4. Nested `newScopedSpan()` calls automatically create proper parent-child relationships.

### API Reference

#### TraceContext APIs
| API Name | Description |
|----------|-------------|
| `TraceContext.newScopedSpan()` | Creates a new child span within the current tracing context. Returns an `ISpan` that should be used in try-with-resources. |
| `TraceContext.newTrace(String operationName)` | Creates a new trace scope builder for the given operation. Returns a `TraceScopeBuilder` for configuration. |
| `TraceContext.currentTraceId()` | Gets the trace ID for the current request. Returns `null` if tracing is not enabled or request is not sampled. |
| `TraceContext.currentSpanId()` | Gets the span ID for the current span. Returns `null` if tracing is not enabled or request is not sampled. |

#### ITraceScope APIs
| API Name | Description |
|----------|-------------|
| `currentTraceId()` | Gets the trace ID associated with this scope. |
| `tracingMode()` | Gets the tracing mode for this scope. |
| `currentSpan()` | Gets the current span for direct manipulation. Each call returns a new instance. |
| `close()` | Finishes the underlying span and detaches the tracing context from the current thread. |

| API Name | Description |
|----------|-------------|
| **Span Identity** | |
| `traceId()` | Gets the trace ID of this span. |
| `spanId()` | Gets the span ID of this span. |
| `parentId()` | Gets the parent span ID of this span. |
| **Span Metadata** | |
| `name()` / `name(String name)` | Gets or sets the span name. |
| `kind()` / `kind(SpanKind kind)` | Gets or sets the span kind. |
| `clazz(String className)` | Sets the class name associated with this span. |
| `method(Class<?> clazz, String method)` | Sets the method information using class and method name. |
| `method(String className, String method)` | Sets the method information using class name and method name. |
| `method(Executable method)` | Sets the method information using reflection. |
| **Span Tags** | |
| `tag(String name, String value)` | Adds a string tag to the span. |
| `tag(String name, int value)` | Adds an integer tag to the span. |
| `tag(String name, long value)` | Adds a long tag to the span. |
| `tag(String name, Object value)` | Adds an object tag to the span (calls toString()). |
| `tag(String name, SocketAddress address)` | Adds a socket address tag to the span. |
| `tag(Throwable exception)` | Adds exception information (type, message, stacktrace) to the span. |
| `tags()` | Gets all tags as a Map<String, String>. |
| **Span Lifecycle** | |
| `start()` | Explicitly starts the span (usually automatic). |
| `startTime()` | Gets the start timestamp in microseconds. |
| `endTime()` | Gets the end timestamp in microseconds. |
| `finish()` | Finishes the span (safe to call multiple times). |
| `close()` | Closes the span (implements AutoCloseable). |

#### TracingMode Enum Values
| Value | Description |
|-------|-------------|
| `TracingMode.TRACING` | Full tracing with span collection and reporting (default). |
| `TracingMode.LOGGING` | Logging-only mode without span collection. |


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
