
# Built-in metrics

TODO...

# Application defined metrics

Bithon also provides a SDK to allow users to define their own metrics. 
The SDK will collect all user-defined metrics and send the metrics to Bithon's collector automatically.

Following steps guides how to use this SDK

1. Add Bithon SDK dependency to your application
   ```xml
    <dependency>
      <groupId>org.bithon.agent</groupId>
      <artifactId>agent-sdk</artifactId>
      <version>1.0-SNAPSHOT</version>
      <scope>provided</scope>
    </dependency>
   ```
2. Define metrics in your application
   ```java
   @Data
    public class GatewayMetrics {
        private LongSum requestTotal = new LongSum();
        private LongSum request4xx = new LongSum();
        private LongSum request5xx = new LongSum();
        private LongSum requestSuccess = new LongSum();
    
        private final LongMax maxResponseTime = new LongMax();
        private final LongMin minResponseTime = new LongMin();
        private final LongSum responseTime = new LongSum();
    }
   ```

3. Register your metrics to Bithon by its public interface

    ```java
    @Bean("GatewayMetricsRegistry")
    IMetricsRegistry<GatewayMetrics> createRegistry() {
        return MetricRegistryFactory.create("gateway-forwarding",
                                            // demensions
                                            Arrays.asList("sourceIp", 
                                                          "url", 
                                                          "cluster", 
                                                          "user", 
                                                          "targetIp", 
                                                          "queryType"),
                                            GatewayMetrics.class);
    }
    ```
   
4. Update metrics at somewhere in your code

   ```java
   @Service
   public class GateListener {
        @Autowired
        IMetricsRegistry<GatewayMetrics> gatewayMetrics;
   
        void onResponse(Request request, Response response) {
            GatewayMetrics metrics = gatewayMetrics.getOrCreate(request.getSourceIp(),
                                                                request.getURI(),
                                                                request.getCluster(),
                                                                request.getUser(),
                                                                request.getTargetIp(),
                                                                request.getQueryType());
   
            metrics.getRequestTotal().update(1);
            metrics.getMaxResponseTime().update(response.getResponseTime());
            metrics.getMinResponseTime().update(response.getResponseTime());
        }
   }
   ```

## Register a callback style metric

   ```java
   @Data
    public class GatewayMetrics {
        public IMetricValueProvider activeConnections;
        public IMetricValueProvider totalConnections;
    }

    GatewayMetrics metrics = gatewayMetrics.getPermanent(request.getTargetIp());
    metrics.activeConnections = ()->{ someClass.getActiveConnection(); };
   ```
   