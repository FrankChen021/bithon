
# Past, Now, and Future

Frank Chen, 2022-01-29

## Past 
### Metrics based platform

When talking about observability, nowadays people may ask what's the difference between opentelmetry and Skywalking or pinpoint.

From the timeline, Bithon can be dated back to 2017 when the distributed tracing just started around the world. 
But at that time, we didn't aim to the distributed tracing feature at first because zipkin was a more popular solution in this domain.
We aimed to provide a solution for the purpose of Java application performance monitoring, more specific, in the metrics domain, not the tracing domain.

As for Java, the best approach to that is by leveraging the java agent technology, which is widely adopted by many similar solutions such as PinPoint.
In the agent domain, JavaAssist and ByteBuddy are two main alternatives, and former is used by PinPoint and the latter is used in our previous products.

In the early version, we integrated many metrics of different middleware clients which were widely used in Java-based internet applications, such as redis, HTTP client, HTTP server, MySQL and Kafka etc.
In 2019, we have set up a sophisticated platform to monitor all Java applications in our product line, about 500 applications and 5000 instances in whole.
The metrics based platform served a key role in daily problem-solving and business predication and business instances expansion before big scale campaign.

### Metrics and Tracing

#### Separated Metrics and Tracing
But we also found that metrics were not enough to address some specific problems. So we introduced tracing capability based on the zipkin's library which was then integrated in our agent.
But the tracing capability was not mature, it's underlying storage is elastic search, and we found that it could bear high volume data ingestion and also the UI was very simple.
So we made some changes to our tracing solution by adopting PinPoint as the service provider of such feature.

PinPoint is a very powerful distributed-tracing tool. Its UI is very powerful. It's underlying storage is HBase which can holds very large data and has been proved success in big data domain.
Also, we had big data team which can cover such technology stack by our own.
At that point, we also had another two alternatives, one was using Skywalking as a replacement of old zipkin based tracing tool, another was to continue improving the zipkin based tracing feature.

For the Skywalking solution, since its underlying storage was also elastic search, which we really didn't like considering high volume data, another reason we didn't take it into production was that we were not familiar with it at all.
For the zipkin based tracing feature, the key problem was still the storage. It was impossible for us to migrate its storage from Elastic Search to HBase or some other storages and put lots of efforts on improving the UI.

So at this stage, we actually had two agents running at the same time, one for metrics, one for tracing provided by PinPoint. It's not a problem and acceptable just looks like a little ugly from the architecture's view.
Since the PinPoint has been mature for a long time, it did not take us much time integrate the tracing capability in our monitoring platform. 
And now users could view different metrics as well as a specific tracing.

#### Integrated Metrics and Tracing

If you have the experience of addressing problems in production, you know that the first step we check a problem is to check the application metrics. 
If something goes wrong or abnormal, you need to dive into details at the time range when the metrics go wrong.

Metrics does not provide the 'deep diving' capability for they're highly aggregated data. So, this is where tracing and logging meet the gap.
As you can see here, metrics and tracing and logging are associated here. We need the ability that we can dive into the tracing or logging at the time range when metrics go wrong.

In 2020 Q1, we made it -- metrics,tracing data,logs, even the all 3 data were actually served by different systems and storages behind, were linked together.
We could quickly check tracing data in a specific time range and some other condition based on the metrics. 
We could also check the logs associated with a specific tracing. We could also check the logs directly from the metrics.

### Alert

For an application monitoring tool, especially a metric based tool, alerting is also one of the pieces. 
The alerting feature served well on the first day of metrics platform was deployed. 
But the problem is, when an alert is raised, it takes us time to check the long term trend of related metrics. We need to link them together to improve the problem-solving efficiency.

It was around 2020 Q4 that the completely new alerting feature, which was highly integrated with the metrics visual platform, was deployed.


### Control Plane

Metrics, tracing, logging are the data as corner stone, if we don't use them, they're useless. Alerting is a case that make the metrics widely useful and active.
As such platform serves more and more important in production, we also want it to meet some needs to govern the whole microservices based applications.
So we did many things based on the metrics data to allow us to control the behavior of applications to make the applications to be more reliable.

### Summary

So, based on our experience, metrics, tracing, logging, alerting, controlling are the 5 key aspects of a monitoring-based tool. 
Nowadays, they're many tools to serve one of 5 requirements well. 
For example, PinPoint is popular in the tracing domain, Elasticsearch dominates the logs' world, prometheus is widely used for metrics storage.
But if you want to find one solution that integrate such 5 key abilities, you cannot find one.

Recently, we're seeing opentelemetry is trying to integrate metrics and tracing and logging together, and Skywalking is also on this way.
But if you really take a deep look at it, they're still tracing-centered. 
They do provide some APIs to allows to you export applications metrics, but it's you that have to do that. The metrics part is not mature solution.

## Metrics VS Tracing

Why are metrics so emphasized over tracing? As I said, metrics are the key part to allow you to see what your application is behaving. 
This is also why Grafana goes viral because it provides a convenient way to allow users to visualize their metric instantly.

Opentelemetry and PinPoint also provides some metrics based the tracing data. But these metrics are only part of the whole system.
We know that tracing data is not 100% sampled, which means the metrics over the tracing data also reflects part of the system.
And there are also many background tasks, components which are not triggered by tracing, the tracing has no ability to help you to get these metrics for you now.

## Future, What's Bithon going to do

Bithon is based on my previous work heavily on the metrics part, and integration of tracing, logging, alerting and controlling. 
Its purpose is to provide an out-of-box solution to help monitor and govern Java based microservices. 

It's going to:

- [X] provide metrics which have nothing to do with tracing feature
- [X] provide tracing capability, which is compatible with both zipkin and opentelemetry trace specification
- [DOING] alerting on metrics
- [Partial] control on applications
- [Partial] integration of metrics, tracing, alerting, control and logging

It's NOT going to:
1. provide a way to collect application log

This is because collecting log via built-in transport channel is not a very good way under modern application deployment especially under Cloud Native environment.
But we are going to provide a way to associate metrics and tracing together.
