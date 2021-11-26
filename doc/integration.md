
## Integration with ClickHouse's trace log

Bithon can be used to collect span logs generated in ClickHouse nodes.

1. Enable span logs on ClickHouse node
   ```xml
   <!--
    OpenTelemetry log contains OpenTelemetry trace spans.
   -->
   <opentelemetry_span_log>
   <!--
       The default table creation code is insufficient, this <engine> spec
       is a workaround. There is no 'event_time' for this log, but two times,
       start and finish. It is sorted by finish time, to avoid inserting
       data too far away in the past (probably we can sometimes insert a span
       that is seconds earlier than the last span in the table, due to a race
       between several spans inserted in parallel). This gives the spans a
       global order that we can use to e.g. retry insertion into some external
       system.
   -->
   <engine>
   engine MergeTree
   partition by toYYYYMM(finish_date)
   order by (finish_date, finish_time_us, trace_id)
   </engine>
   <database>system</database>
   <table>opentelemetry_span_log</table>
   <flush_interval_milliseconds>7500</flush_interval_milliseconds>
   </opentelemetry_span_log>
   ```
2. create a materialized view to export span logs from ClickHouse to Bithon
    ```sql
   CREATE MATERIALIZED VIEW span_logs_view
    (
        `appName` String,
        `instanceName` String,
        `traceId` String,
        `parentSpanId` String,
        `spanId` String,
        `method` String,
        `startTime` UInt64,
        `endTime` UInt64,
        `costTime` Int64,
        `tags` Map(String, String)
    )
    ENGINE = URL('http://{YOUR_BITHON_COLLECTOR}:9897/api/collector/trace', 'JSONEachRow')
    SETTINGS output_format_json_named_tuples_as_objects = 1, output_format_json_array_of_rows = 1 AS
    SELECT
        'clickhouse' AS appName,
        concat(FQDN(), ':8123') AS instanceName,
        lower(hex(reinterpretAsFixedString(trace_id))) AS traceId,
        lower(hex(parent_span_id)) AS parentSpanId,
        lower(hex(span_id)) AS spanId,
        operation_name AS method,
        start_time_us AS startTime,
        finish_time_us AS endTime,
        finish_time_us - start_time_us AS costTime,
        CAST(tuple('clickhouse'), 'Tuple(serviceName text)') AS localEndpoint,
        attribute AS tags
    FROM 
        system.opentelemetry_span_log
    ```
