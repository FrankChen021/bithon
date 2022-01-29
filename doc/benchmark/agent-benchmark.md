
#  Benchmark

This article describes the performance loss caused by the java agent that is attached to target applications.

The performance loss comes from several aspects.
1. The time spent on code injection to target methods of some class when a specific class is loaded into class loader
2. The injected code that is around some specific methods of some classes to finish the metrics and tracing data collection
3. The background work that the agent is doing for sending the collected data to remote servers

## Evaluation Method

The 1st slows the startup of an application. How many seconds that it may take depends on how many classes will be loaded into the class loader during the startup phase and how many plugins will take effect. Obviously, the more classes an application will load, the more time it needs to start up the application.

To evaluate such performance loss, we just pick up some application without any special purposes and then compare the startup time before and after the java is attached.

For the 2nd the 3rd performance issues, it's not easy to separate them for evaluation respectively. So let's consider them together.

Since the injected code are mainly on HTTP request call paths, we can evaluate the overhead by benchmark the QPS of a HTTP interface before and after the agent is attached to see how it impact the QPS of an interface. Also, we will monitor the CPU and memory usage during this phase to compare the overhead.

## Target Application

We select target applications without any special purposes. The test can be done on any applications.

Here let's pick Apache Druid as the target application. Apache Druid provides multiple different kinds of processes, we can see how the agent slows the startup for different processes.

## Evaluation Steps

### Startup overhead

1. start up the nano configuration of Apache Druid cluster(coordinator, overlord, middle-manager, historical, router, broker), check their logs to see when the applications start to specific point
2. attache the java agent to the target applications by the default configuration, start up the cluster again to see how long the processes take to finish the startup

### QPS loss

#### Deployment

```mermaid
    flowchart LR
        subgraph macOS#1
            router
            coordinator
            middle-manager
            broker
            historical
            peon
            test-code
        end
        subgraph macOS#2
            Bithon
        end
        coordinator --metrics/tracing--> Bithon
        middle-manager --metrics/tracing--> Bithon
        router --metrics/tracing--> Bithon
        broker --metrics/tracing--> Bithon
        historical --metrics/tracing--> Bithon
        peon --metrics/tracing--> Bithon
        test-code --requests--> router
```

> NOTE
> 
> The test-code is running on the same machine of the target applications,
> this is to eliminate network latency between two macOS which are under same WiFi network.
> Even they're under the same WiFi 5GHz channel network, the latency is still unstable.

#### Hardware Configurations

- macOS#1
  - MacBookPro 2019, 2.6 GHz 6-Core Intel Core i7, 16 GB 2667 MHz DDR4

- macOS#2
  - MacBookPro 2018, 2.6 GHz 6-Core Intel Core i7, 16 GB 2400 MHz DDR4

#### Steps
1. start upt the nano configuration of Apache Druid with the java agent that only enable the JVM plugin to collect Java application's basic metrics such CPU usage and memory usage
   1. Issue query request on the wikipedia data source with different intervals from a client repeatedly to get the QPS

2. start up the nano configuration of Apache Druid with java agent's default configuration which by default will load all plugins, do the test as described step 1 again to check the QPS

3. start up the nano configuration of Apache Druid with java agent's default configuration which by default will load all plugins, set the sampling rate of tracing to 100%, do the test as described as step 1 again to evaluate the QPS

## Startup Test Result

Once the application start, grep the 'Started' keyword to see the how long the application takes to start up. Following tables illustrates the result.

| Process              |  No Agent Attached(ms)  |  Bithon Agent Attached  |  Skywalking 8.8.0 Agent Attached  |
|----------------------|:-----------------------:|:-----------------------:|:---------------------------------:|
| broker               |          11900          |          20639          |               45161               |
 | coordinator-overlord |          11561          |          19463          |               43820               |
 | historical           |          11145          |          19109          |               43046               |
 | middleManager        |          10521          |          18206          |               42588               |
| router               |          10255          |          17888          |               41751               |

## Performance Overhead


### Test Code

```java
Timer timer = Timer.builder("timer")
                   .publishPercentiles(0.95, 0.99)
                   .publishPercentileHistogram()
                   .serviceLevelObjectives(Duration.ofMillis(100))
                   .minimumExpectedValue(Duration.ofMillis(1))
                   .maximumExpectedValue(Duration.ofSeconds(10))
                   .register(registry);

while (counter++ < 1000) {
    query.query = String.format("SELECT * FROM wikipedia WHERE __time > MILLIS_TO_TIMESTAMP(%d) LIMIT 10", timestamp++);
    
    long s = System.nanoTime();
    URI uri = restTemplate.postForLocation("http://192.168.2.50:8888/druid/v2/sql", query);
    long e = System.nanoTime();
    
    long t = e - s;
    min = Math.min(min, t);
    timer.record(t, TimeUnit.NANOSECONDS);
}
```

#### Execution Flow
The SQL is sent to Apache Druid's router process, and then will be forwarded to broker process. 
And the broker process will turn the SQL query into a Druid's native JSON query which is executed on historical process.

The execution flow is demonstrated as follows, which is illustrated on the trace map of a trace from Bithon.

![trace-topo](trace-topo.png)

### Baseline data

Following table shows the test result of target application without the agent attached. It can be seen as the baseline.

| Min(ms)  | Max(ms)    | Avg(ms)   | P95(ms)   | P99(ms)   | Total(ms)    | QPS        |
|----------|------------|-----------|-----------|-----------|--------------|------------|
| 6.629687 | 259.565178 | 12.670067 | 24.903680 | 33.292288 | 12670.066548 | 78.926184  |
| 6.652671 | 63.801387  | 10.232591 | 15.990784 | 21.757952 | 10232.591163 | 97.726957  |
| 6.571454 | 100.769671 | 10.026595 | 15.466496 | 21.757952 | 10026.595042 | 99.734755  |
| 6.337419 | 65.668054  | 9.510715  | 13.893632 | 16.515072 | 9510.715252  | 105.144563 |
| 6.460049 | 68.611847  | 9.548022  | 14.417920 | 19.660800 | 9548.022110  | 104.733733 |
| 6.339690 | 66.701136  | 9.360598  | 13.893632 | 18.612224 | 9360.598283  | 106.830778 |
| 6.100043 | 70.867612  | 9.184785  | 13.369344 | 17.563648 | 9184.784936  | 108.875712 |
| 6.334391 | 66.943560  | 9.180635  | 13.893632 | 17.563648 | 9180.635342  | 108.924923 |

The first row reflects how the target application perform when they receive first batch of requests.
This means the application run some extra code path to initialize the system such as web container.
So it does not reflect the best performance of the system, and should be ignored.

### 100% Sample Rate

The agent is configured to collect all tracing data for all requests by setting this parameter `-Dbithon.tracing.sampleRate=100` whose value by default is 0.

| Min(ms)  | Max(ms)     | Avg(ms)   | P95(ms)   | P99(ms)   | Total(ms)    | QPS        |
|----------|-------------|-----------|-----------|-----------|--------------|------------|
| 9.841467 | 1598.517763 | 21.992187 | 37.224448 | 49.807360 | 21992.187414 | 45.470693  |
| 7.825205 | 68.539659   | 14.186356 | 25.952256 | 33.292288 | 14186.355529 | 70.490268  |
| 7.454702 | 65.591494   | 12.011027 | 19.660800 | 24.903680 | 12011.026735 | 83.256829  |
| 7.260074 | 69.390262   | 11.011897 | 16.515072 | 20.709376 | 11011.897113 | 90.810874  |
| 7.176657 | 65.607880   | 10.691981 | 15.990784 | 23.855104 | 10691.980970 | 93.528038  |
| 6.867503 | 69.031957   | 10.593516 | 17.563648 | 20.709376 | 10593.516474 | 94.397361  |
| 6.536093 | 62.991241   | 10.341561 | 17.563648 | 19.660800 | 10341.561371 | 96.697197  |
| 6.389569 | 70.144868   | 9.916755  | 14.942208 | 17.563648 | 9916.755239, | 100.839435 |
| 6.590661 | 81.347927   | 9.851637  | 14.942208 | 19.660800 | 9851.637219, | 101.505971 |

The first row in the table is under test that the system is started, it takes mor time to initialize many code path.
So it's valueless to compare.

If we look at the `Min` column we can see that even under 100% sample rate, a response can be as soon as 6.38ms, only 0.28ms plus compared to the minimum value of the baseline table.
And the system can still serve 1000 requests with 9851ms, about 700ms more compared to the lowest total time consumption, 9180ms, of the baseline.
And only 0.7ms averaged to each request.

According to the rationality of the agent, the extra time spending is only related to a request itself, it can be seen as a fixed value.
This value does not fluctuate how long a request takes.
This means, the longer time a request takes, the less QPS impact the agent can cause.

### CPU Consumption

