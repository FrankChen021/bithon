

## Architecture

```mermaid
    flowchart TD
        alerting-manager -. Create/Delete Alert .-> Storage
        alerting-evaluator -. Read Alert .-> Storage
        alerting-evaluator -. Read Metrics .-> bithon-server
        alerting-evaluator -. Generate images .-> image-render-service
        alerting-evaluator -. Notification(HTTP) .-> external-application
        alerting-evaluator -. Notification(Kafka) .-> Kafka
```

## Alert Rule

### Template

The alert in Bithon is defined as the following template in YAML.

```yaml
id: "f7f87ce1e0b444919b123849f4c7939f"
checkApplication: "false"
appName: "optional, the name of application that the alert belongs to"

name: "the name of the alert"
expr: "PromQL style expression, for example: avg(jvm-metrics.processCpuLoad)[1m] > 0.1[-1h]"
for: "The duration before the alert should be fired. In the format of duration. Like 1m, 1h"
every: "Optional, the interval of evaluation in minutes"
notificationProps:
  channels: [name list of notification channels]
```

### Expression Syntax

```text
expression: aggregator(datasource.metric{dim1='val1' AND dim2='val2'})[duration] op expected_value
aggregator: 'sum'|'avg'|'min'|'max'|'first'|'last'|'count'
duration: number 's'|'m'|'h'|'d'
op: '>'|'>='|'<'|'<='|'<>'|'='
expected_value: number | percentage
percentage: number '%'
```

### Example

There are some Scratch Files under the [manager module](manager) directory. 
You can directly run these scratch files in Intellij to see how the APIs work.
