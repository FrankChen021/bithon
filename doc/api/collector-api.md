# Bithon Collector APIs

This document describes the APIs exposed by the Bithon collector module to receive metrics and tracing span logs from external applications.

## HTTP Collector APIs

The HTTP collector provides endpoints for receiving metrics and traces in Bithon's native format.

### Metrics API

**Endpoint:** `/api/collector/metrics`
**Method:** POST
**Content-Type:** application/json
**Default Port:** 9897

#### Request Format

```json
{
  "schema": {
    "name": "data_source_name",
    "dimensionsSpec": [
      {
        "name": "dim1",
        "type": "string"
      },
      {
        "name": "dim2",
        "type": "string"
      }
    ],
    "metricsSpec": [
      {
        "name": "metric1",
        "type": "long"
      },
      {
        "name": "metric2",
        "type": "double"
      }
    ]
  },
  "metrics": [
    {
      "timestamp": 1636546800000,
      "interval": 60000,
      "dimensions": {
        "dim1": "value1",
        "dim2": "value2"
      },
      "metrics": {
        "metric1": 100,
        "metric2": 200.5
      }
    }
  ]
}
```

#### Description

- `schema`: Defines the metadata for the metrics being sent
    - `name`: Name of the schema(data source)
    - `dimensionsSpec`: Dimension definitions including name and type
    - `metricsSpec`: Metric definitions including name and type. Type can be either `long` or `double`.
- `metrics`: Array of metric measurements
    - `timestamp`: Timestamp in milliseconds
    - `interval`: Collection interval in milliseconds
    - `dimensions`: Key-value pairs for dimensions
    - `metrics`: Key-value pairs for the metric values (numbers only)

### Traces API

**Endpoint:** `/api/collector/trace`  
**Method:** POST  
**Content-Type:** application/json
**Default Port:** 9897

##### Content Encodings

The collector supports the following content encodings for both formats:

- gzip
- deflate
- lz4 (FramedLZ4 only)

##### Span Object Fields

The endpoint receives tracing span logs in the JSON format, each object represents a tracing span with the following structure:

| Field Name     | Type   | Required | Description                                                                |
|----------------|--------|----------|----------------------------------------------------------------------------|
| `appName`      | string | optional | Application name                                                           |
| `instanceName` | string | optional | Instance identifier                                                        |
| `traceId`      | string | required | Unique identifier for the trace                                            |
| `spanId`       | string | required | Unique identifier for the span                                             |
| `kind`         | string | optional | Span kind - one of: `SERVER`, `CLIENT`, `PRODUCER`, `CONSUMER`, `INTERNAL` |
| `parentSpanId` | string | optional | Parent span identifier for creating span hierarchy                         |
| `tags`         | object | optional | Key-value pairs for span attributes/tags                                   |
| `costTime`     | long   | required | Duration in microseconds                                                   |
| `startTime`    | long   | required | Start timestamp in **microseconds**                                        |
| `name`         | string | required | Operation name or description                                              |
| `clazz`        | string | optional | Java class name (for Java applications)                                    |
| `method`       | string | optional | Method name (for Java applications)                                        |
| `status`       | string | optional | Span status (e.g., OK, ERROR, TIMEOUT). Defaults to empty string           |

**Important Notes:**

- `costTime` and `startTime` are in **microseconds**, not milliseconds
- The `tags` field should be a flat key-value object where both keys and values are strings

##### Request Format

This endpoint support two different data formats:

- JSON Array Format
  The format contain an array of trace span objects with above structure.
- List of JSON Object Format
  In this format, the text contains one or more JSON objects each of which represents a single span. This format is more memory-efficient for large batches for client to send tracing logs in high-throughput way.

This endpoint automatically detects the format with any performance loss based on the request content:

- If the first character is `[`, it's treated as JSON Array format
- If the first character is `{`, it's treated as List of JSON Object format

There's no performance difference between these two during deserialization. But JSONEachRow format is very suitable for large requests, the client can send tracing logs in streaming way.

##### 1. JSON Array Format

In this case, the whole request body is a JSON array containing multiple span objects.

```json
[
  {
    "appName": "user-service",
    "instanceName": "user-service-01",
    "appType": "JAVA",
    "traceId": "1a2b3c4d5e6f7890",
    "spanId": "abcd1234efgh5678",
    "kind": "SERVER",
    "name": "GET /api/users",
    "startTime": 1636546800000000,
    "costTime": 150000,
    "endTime": 1636546800150000,
    "status": "OK",
    "normalizedUri": "/api/users",
    "clazz": "com.example.UserController",
    "method": "getUsers",
    "tags": {
      "http.method": "GET",
      "http.url": "/api/users",
      "http.status_code": "200"
    }
  },
  {
    "appName": "user-service",
    "instanceName": "user-service-01",
    "traceId": "1a2b3c4d5e6f7890",
    "spanId": "ijkl9012mnop3456",
    "parentSpanId": "abcd1234efgh5678",
    "kind": "CLIENT",
    "name": "SELECT users",
    "startTime": 1636546800050000,
    "costTime": 30000,
    "endTime": 1636546800080000,
    "status": "OK",
    "clazz": "com.example.UserRepository",
    "method": "findAllUsers",
    "tags": {
      "db.type": "mysql",
      "db.statement": "SELECT * FROM users",
      "db.connection_string": "mysql://localhost:3306/userdb"
    }
  }
]
```

##### Example Usage

```bash
curl -X POST http://localhost:9897/api/collector/trace \
  -H "Content-Type: application/json" \
  -d '[{"appName":"test-service","traceId":"abc123","spanId":"def456","name":"test","startTime":1636546800000000,"costTime":100000}]'
```

##### 2. List of JSON Objects (Streaming) **Highly Recommended**

In this case, the request body contains multiple JSON objects, each representing a single span.
Each JSON object can be in one line or multiple lines, but must be valid JSON. No special characters are required to separate them.

The below is an example of a request body with multiple spans each of which is in a single line and new line character is used to separate them for demonstration purposes.
In practice, you can send them in a streaming way without new line character.

```
{"appName": "user-service", "instanceName": "user-service-01", "traceId": "1a2b3c4d5e6f7890", "spanId": "abcd1234efgh5678", "kind": "SERVER", "name": "GET /api/users", "startTime": 1636546800000000, "costTime": 150000, "status": "OK", "clazz": "com.example.UserController", "method": "getUsers", "tags": {"http.method": "GET", "http.url": "/api/users", "http.status_code": "200"}}
{"appName": "user-service", "instanceName": "user-service-01", "traceId": "1a2b3c4d5e6f7890", "spanId": "ijkl9012mnop3456", "parentSpanId": "abcd1234efgh5678", "kind": "CLIENT", "name": "SELECT users", "startTime": 1636546800050000, "costTime": 30000, "status": "OK", "clazz": "com.example.UserRepository", "method": "findAllUsers", "tags": {"db.type": "mysql", "db.statement": "SELECT * FROM users"}}
{"appName": "order-service", "instanceName": "order-service-02", "traceId": "9z8y7x6w5v4u3210", "spanId": "qrst5678uvwx9012", "kind": "SERVER", "name": "POST /api/orders", "startTime": 1636546801000000, "costTime": 200000, "status": "OK", "clazz": "com.example.OrderController", "method": "createOrder", "tags": {"http.method": "POST", "http.url": "/api/orders", "http.status_code": "201"}}
```

##### Example Usage

```bash
curl -X POST http://localhost:9897/api/collector/trace \
  -H "Content-Type: application/json" \
  -d '{"appName":"test-service","traceId":"abc123","spanId":"def456","name":"test","startTime":1636546800000000,"costTime":100000}
{"appName":"test-service","traceId":"abc123","spanId":"ghi789","parentSpanId":"def456","name":"child","startTime":1636546800050000,"costTime":50000}'
```

## OpenTelemetry (OTLP) Collector APIs

Bithon supports OpenTelemetry Protocol (OTLP) for collecting traces via both HTTP and gRPC.

### OTLP HTTP API

**Endpoint:** `/api/collector/otlp/trace`  
**Method:** POST
**Default Port:** 9897

#### Supported Content Types

- `application/x-protobuf`: Binary protobuf format
- `application/json`: JSON format

#### Supported Content Encodings

- gzip
- deflate

Here's a [sample JSON-formatted data](https://github.com/FrankChen021/opentelemetry-proto/blob/6979341897b78d695377d08200e79f060384b7b4/examples/trace.json) that you can use to test the API.

### OTLP gRPC API

**Default Port:** 4317

The gRPC endpoint implements the OpenTelemetry TraceService as defined in the [OTLP specification](https://opentelemetry.io/docs/specs/otlp/#otlpgrpc).

## Zipkin Collector API

Bithon supports receiving traces in Zipkin v2 format.

### Zipkin API

**Endpoints:**

- `/api/collector/zipkin/v2/spans` (The endpoint for standard Zipkin endpoint `/api/v2/spans`)

**Method:** POST
**Default Port:** 9897

#### Supported Content Type

- `application/json`: JSON format

#### Supported Content Encodings

- gzip
- deflate

#### Request Format

Zipkin spans follow the [Zipkin v2 JSON format](https://zipkin.io/zipkin-api/zipkin2-api.yaml):

```json
[
  {
    "traceId": "d9cda95b652f4a1592b449d5929fda1b",
    "parentId": "bd7a977555f6b982",
    "id": "be2d01e33cc78d97",
    "kind": "SERVER",
    "name": "get /api",
    "timestamp": 1472470996199000,
    "duration": 207000,
    "localEndpoint": {
      "serviceName": "frontend",
      "ipv4": "127.0.0.1",
      "port": 8080
    },
    "remoteEndpoint": {
      "serviceName": "backend",
      "ipv4": "192.168.99.101",
      "port": 9000
    },
    "tags": {
      "http.method": "GET",
      "http.path": "/api"
    }
  }
]
```

#### Configuration Options

The Zipkin trace collector is enabled with:

- `bithon.receivers.traces.zipkin.enabled=true`

## Jaeger Collector APIs

Bithon supports receiving traces in Jaeger format via both UDP and HTTP protocols.

### Jaeger UDP API

**Protocol:** UDP  
**Default Port:** 6831  
**Format:** Thrift Compact Protocol

The UDP endpoint receives Jaeger traces in Thrift compact binary format, which is the standard format used by Jaeger clients for high-performance trace submission.

#### Features

- High-performance UDP transport for minimal latency
- Automatic thread pool sizing based on system capabilities
- Thrift compact protocol deserialization
- Batch processing for memory efficiency

#### Configuration Options

The Jaeger UDP trace collector supports the following configuration:

- `bithon.receivers.traces.jaeger-udp.enabled=true`
- `bithon.receivers.traces.jaeger-udp.port=6831` (default)
- `bithon.receivers.traces.jaeger-udp.threads=<auto>` (automatically calculated based on CPU cores)

### Jaeger HTTP API

**Endpoints:**

- `/api/collector/jaeger/traces` (Standard Jaeger collector HTTP endpoint)

**Method:** POST  
**Default Port:** 9897

#### Supported Content Types

- `application/vnd.apache.thrift.binary`: Thrift binary protocol
- `application/vnd.apache.thrift.compact`: Thrift compact protocol

#### Supported Content Encodings

- gzip
- deflate

#### Request Format

The HTTP endpoint accepts Jaeger traces in Thrift binary format. The request body should contain a serialized Jaeger `Batch` object containing:

- `process`: Service information including service name and tags
- `spans`: Array of Jaeger span objects with trace/span IDs, operation names, timestamps, durations, and tags

#### Features

- Support for both Thrift binary and compact protocols
- Compression support (gzip, deflate)
- Stream processing for memory efficiency
- Batch processing with configurable limits
- Proper HTTP status codes and error handling

#### Configuration Options

The Jaeger HTTP trace collector is enabled with:

- `bithon.receivers.traces.jaeger-http.enabled=true`

#### Example Usage

To send traces to the Jaeger HTTP endpoint, configure your Jaeger client to use:

```
JAEGER_ENDPOINT=http://your-bithon-server:9897/api/collector/jaeger/traces
```

## Configuration Examples

To enable all collectors, add the following to your configuration:

```yaml
bithon:
  receivers:
    metrics:
      http:
        enabled: true
    traces:
      http:
        enabled: true
        maxRowsPerBatch: 1000
      otlp-http:
        enabled: true
      otlp-grpc:
        enabled: true
        port: 4317
      zipkin:
        enabled: true
      jaeger-udp:
        enabled: true
        port: 6831
        threads: 8
      jaeger-http:
        enabled: true
```

You can selectively enable only the collectors you need by setting their respective `enabled` properties to `true`.
 