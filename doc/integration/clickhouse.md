
## Collect ClickHouse's trace log

Bithon can be used to collect span logs generated in ClickHouse nodes.

1. Make sure you're using the right ClickHouse

    There are some PRs contributed to ClickHouse to make the tracing inside ClickHouse works well. 

    The latest master branch after 2022-02-03 contains all changes. If you're not using the latest master branch,
you can cherry-pick these PRs to your own branch.

- [Parse and store opentelemetry trace-id in big-endian order ](https://github.com/ClickHouse/ClickHouse/pull/33723)
- [Improve the operation name of an opentelemetry span](https://github.com/ClickHouse/ClickHouse/pull/32234)
- [Allows hex() to work on type of UUID](https://github.com/ClickHouse/ClickHouse/pull/32170)
- [Ignore parse failure of opentelemetry header](https://github.com/ClickHouse/ClickHouse/pull/32116)
- [Set Content-Type in HTTP packets issued from URL engine](https://github.com/ClickHouse/ClickHouse/pull/32113)
- [Returns Content-Type as application/json for JSONEachRow if output_format_json_array_of_rows is enabled](https://github.com/ClickHouse/ClickHouse/pull/32112)
- [Add exception/exception_code to opentelemetry span log](https://github.com/ClickHouse/ClickHouse/pull/32040)
- [Fix a bug that opentelemetry span log duration is zero](https://github.com/ClickHouse/ClickHouse/pull/32038)


2. Enable span logs on ClickHouse node
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
   ttl finish_date + toIntervalDay(1)
   </engine>
   <database>system</database>
   <table>opentelemetry_span_log</table>
   <flush_interval_milliseconds>7500</flush_interval_milliseconds>
   </opentelemetry_span_log>
   ```

   > NOTE: You can change the TTL setting according to your own case. But since the records in this table will be exported via URL engine as below, the TTL in the above example is long enough.   

3. create a materialized view to export span logs from ClickHouse to Bithon
   
   ```sql
   CREATE MATERIALIZED VIEW system.span_logs_view
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
    ENGINE = URL('http://{YOUR_BITHON_COLLECTOR}:9897/api/collector/trace', 'JSONEachRow', 'gzip')
    SETTINGS output_format_json_named_tuples_as_objects = 1, output_format_json_array_of_rows = 1 AS
    SELECT
        'clickhouse' AS appName,
        concat(FQDN(), ':8123') AS instanceName,
        lower(hex(trace_id)) AS traceId,
        case when parent_span_id = 0 then '' else lower(hex(parent_span_id)) end AS parentSpanId,
        lower(hex(span_id)) AS spanId,
        operation_name AS method,
        start_time_us AS startTime,
        finish_time_us AS endTime,
        finish_time_us - start_time_us AS costTime,
        attribute AS tags
    FROM 
        system.opentelemetry_span_log
   ```

> NOTE: 
> if `appName` field is not 'clickhouse', you should configure the application name to bithon-server under 
> `collector-http.tracing.clickHouseApplications` path.
> 
> The above example creates the MV under `system` database, this is not mandatory, you can change it to any database.
> 
> If the compression is enabled for the URL engine(the 'gzip' parameter), make sure your ClickHouse contains this PR: [Fix compression support in URL engine](https://github.com/ClickHouse/ClickHouse/pull/34524). 