# Bithon Collector APIs

This document describes the APIs exposed by the Bithon collector module for receiving telemetry data from external applications.

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
    "name": "schema_name",
    "fields": [
      {"name": "field1", "type": "STRING"},
      {"name": "field2", "type": "LONG"}
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
  - `name`: Name of the schema/metric type
  - `fields`: Field definitions including name and type
- `metrics`: Array of metric measurements
  - `timestamp`: Timestamp in milliseconds
  - `interval`: Collection interval in milliseconds
  - `dimensions`: Key-value pairs for dimensions (tags)
  - `metrics`: Key-value pairs for the metric values (numbers only)

### Traces API

**Endpoint:** `/api/collector/trace`  
**Method:** POST  
**Content-Type:** application/json
**Default Port:** 9897  

#### Request Format

The request should contain an array of trace span objects. The collector supports the following content encodings:
- gzip
- deflate
- lz4 (FramedLZ4 only)

```json
[
  {
    "traceId": "trace_id_value",
    "spanId": "span_id_value",
    "parentSpanId": "parent_span_id_value",
    "name": "span_name",
    "kind": "SERVER",
    "timestamp": 1636546800000,
    "duration": 100,
    "tags": {
      "tag1": "value1",
      "tag2": "value2"
    }
  }
]
```

#### Configuration Options

The HTTP trace collector supports the following configuration:
- `bithon.receivers.traces.http.maxRowsPerBatch`: Maximum number of spans to process in a single batch

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
```

You can selectively enable only the collectors you need by setting their respective `enabled` properties to `true`.
 