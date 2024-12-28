
#
The Spring WebFlux is used in Spring Gateway, which serves as http server and http client.
The http client used in Spring Gateway is based on reactor-netty-httpclient,
which is instrumented by httpclient-reactor plugin.

So, to make the tracing and metrics of Spring Gateway works,
we also need to make sure the httpclient-reactor plugin is enabled.

## Trace Propagation Path

```mermaid
graph 
    ReactorHttpHandlerAdapter(HttpServerOperations, resp)
    ---> filters ( AbstractServerHttpRequest(HttpServerOperations) )
    ---> NettyRoutingFilter 
    ---> HttpClientFinializer#send
    ---> HttpClientFinializer#responseConnection
    ---> Flux#timeout
```

1. TraceContext is created on `ReactorHttpHandlerAdapter`
2. TraceContext is held in thread local for following interceptors to use
3. methods of `HttpClientFinializer` is called in `NettyRoutingFilter`
4. Trace span for `HttpClientFinializer` is created in `HttpClientFinializer#send` and is released in `HttpClientFinializer#responseConnection`