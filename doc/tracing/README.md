
# Supported Components that can initialize tracing context

 1. HTTP Server           
 2. Quartz                
 3. Spring Scheduling     
 4. Spring Kafka Listener 

Different components have different conditions to enable tracing context which is described as follows.

## HTTP Server

As of now, these HTTP servers are supported by Bithon.

- Tomcat
- Jetty
- Undertow
- Netty
- Webflux(Spring Gateway)

The Bithon will initialize the tracing context for an incoming HTTP request when:

- Either HTTP request contains a tracing context, which follows the standard

| Standard | Required HTTP Header                                      |
|----------|-----------------------------------------------------------|
| W3C      | `traceparent`                                             |
| Zipkin   | `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-ParentSpanId`        |
| Pinpoint | `Pinpoint-TraceID`, `Pinpoint-SpanID`, `Pinpoint-pSpanID` |
          
- Or the Bithon determines the tracing context should be initialized by sampling calculation based on `samplingRate` of each component.
See the [Tracing Configuration](../configuration/server/configuration-agent.md#tracing-configurations) of agent for more detail.