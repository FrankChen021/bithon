
# Dashboard Configuration

## Example - Application Overview

```json
{
  "title": "Application Overview",
  "charts": [
    {
      "dataSource": "jvm-metrics",
      "title": "Instance",
      "width": 4,
      "type": "table",
      "query": {
        "type": "groupBy",
        "columns": [
          {"name": "instanceName", "title": "instance" },
          "processors",
          {"name": "instanceStartTime", "title": "startTime", "format": "dateTime" },
          {"name": "instanceUpTime", "title": "upTime", "format": "timeDuration" },
          {"name": "heapUsed", "expression": "round(heapUsed * 100.0/heapMax, 2)", "title": "heapUsed(%)" },
          {"name": "heapMax", "format": "binary_byte" },
          {"name" : "processCpuLoad", "title": "processCpuLoad(%)" }
        ],
        "orderBy": {
          "name": "instanceUpTime",
          "order": "desc"
        }
      }
    },
    {
      "dataSource": "http-incoming-metrics",
      "title": "HTTP Incoming(Top N)",
      "width": 4,
      "type": "table",
      "query": {
        "type": "groupBy",
        "columns": [
          "uri",
          {"name": "totalCount",      "format": "compact_number"},
          {"name": "errorCount",      "format": "compact_number"},
          {"name": "errorRate",       "expression": "round(errorCount*100.0/totalCount, 2)", "title": "errorRate(%)"},
          {"name": "avgResponseTime", "format": "millisecond"}
        ],
        "limit": 10,
        "orderBy": {
          "name": "totalCount",
          "order": "desc"
        }
      }
    }
  ]
}
```

### Column configuration
- title
    
    The text that will be shown in the dashboard.

- name

    The output name of a metric. Can also be used as title if `title` is not given.

- aggregator
- expression
- format
    
    Supported formats:
    - compact_number
    - binary_byte
    - ...

## Example - Get cardinality of application instances

To illustrate cardinality of application instances in chart, add following column to columns of a char description file.

```json
{ "name": "instanceCount", "field": "instanceName", "aggregator": "cardinality" }
```