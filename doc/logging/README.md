
# Logging

Logging support is a one of the most important parts for observability platforms. 
But logging has a long history in the history of application, it came out much earlier than distributed tracing and metrics,
there have been a lot of logging solutions. 

To better cooperate with existing logging systems, Bithon does not collect the application log to save it in a central place so that they can be searched by giving conditions.
Instead, Bithon only injects the trace id for a request to the log. Existing logging system can still work without any change, but by the injection, users can search the log by given trace id.

This document describes the logging support.

## Rationale

Bithon injects trace id to logs automatically, it requires the target application running with `log4j2` or `logback` or `slf4j`(without any Maven Shading).
When target application starts, once Bithon detects these logging libraries are integrated, it will first insert the trace id pattern to the existing user define logging pattern.
When an HTTP request/Kafka message arrives, Bithon will generate a trace id for such request(whenever this request is sampled or not by the `samplingRate` configuration).
Such trace id will be put into the MDC context, and when any log is output, the logging library will cooperate the trace id into the log message.

For example, 

```text
2023-04-22 23:39:54.876  INFO 59110 --- [nio-9897-exec-4] o.b.s.s.jdbc.metric.MetricJdbcReader     : [bTxId: bcde72610a4742d78841757fa55a8636, bSpanId: e6e6f12700000061, bMode: L] Executing SQL...
```

in above output, the message `[bTxId: bcde72610a4742d78841757fa55a8636, bSpanId: e6e6f12700000061, bMode: L]` is automatically injected by Bithon, indicating that 
- the trace id is `bcde72610a4742d78841757fa55a8636`
- the span id is `e6e6f12700000061`
- the mode `L` means this trace id is a logging-only trace id, it's not recorded by the distributed tracing
    - if the mode is `T`, it indicates that the trace id is also an id of distributed tracing, it can be used to search the tracing log

## Features

### Supported logging framework

- log4j2
- logback
- slf4j

### Supported Components

Following entries are supported to initialize a trace id for logging.

- Spring Web Server, including
  - Tomcat Web Server
  - Undertow Web Server
  - Jetty Web Server
  - Web Flux
- Spring Kafka Listener
- Spring Scheduler
- Quartz2
- xxl-job

### Disable trace id injection
If you want to disable the auto trace id injection, you can set the `bithon.logging.disableTraceIdAutoInjection` property to `true` in the configuration file or JVM argument.

```text
# In JVM argument
-Dbithon.logging.disableTraceIdAutoInjection=true
```

```yaml
# In configuration file
bithon:
  logging:
    disableTraceIdAutoInjection: true
```

After the disabling, you can still use MDC to get the trace id but do the trace id injection by yourself.