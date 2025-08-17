# Java 11+ HttpClient Plugin

This plugin instruments the modern `java.net.http.HttpClient` API introduced in Java 11 to collect metrics and propagate tracing spans.

## Features

- **Metrics Collection**: Tracks HTTP request metrics including:
  - Request count and response time
  - HTTP status codes
  - Request/response byte sizes
  - Exception tracking

- **Distributed Tracing**: Propagates tracing spans for:
  - Synchronous HTTP requests (`HttpClient.send()`)
  - Asynchronous HTTP requests (`HttpClient.sendAsync()`)
  - Configurable request/response header tracking

## Intercepted Methods

The plugin intercepts the following methods:

- `java.net.http.HttpClient.send(HttpRequest, HttpResponse.BodyHandler)`
- `java.net.http.HttpClient.sendAsync(HttpRequest, HttpResponse.BodyHandler)`
- `java.net.http.HttpClient.sendAsync(HttpRequest, HttpResponse.BodyHandler, HttpResponse.PushPromiseHandler)`

## Implementation Notes

- For synchronous requests, metrics and tracing are handled directly in the interceptor
- For asynchronous requests, the returned `CompletableFuture` is wrapped to handle completion/exceptions
- Trace headers must be added to the `HttpRequest` before calling `send()` or `sendAsync()` since `HttpHeaders` is immutable
- Response size is calculated from the `content-length` header when available

## Differences from Legacy JDK Plugin

This plugin targets the modern `java.net.http.HttpClient` API (Java 11+), while the existing `jdk` plugin instruments the legacy `java.net.HttpURLConnection` API. Both can coexist in the same application.
